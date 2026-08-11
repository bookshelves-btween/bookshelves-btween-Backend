package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient;
import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.dto.response.BookDetailResDTO;
import com.bookshelves.domain.book.dto.response.BookDetailResDTO.MemberBookInfo;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO.BookInfo;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO.CategoryInfo;
import com.bookshelves.domain.book.dto.response.MemberBookCalendarResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookCalendarResDTO.CalendarDay;
import com.bookshelves.domain.book.dto.response.MemberBookListResDTO;
import com.bookshelves.domain.book.dto.response.MemberBookListResDTO.MemberBookRecord;
import com.bookshelves.domain.book.dto.response.MemberBookStatisticsResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO.RecentSearchInfo;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.book.entity.MemberBookHistory;
import com.bookshelves.domain.book.enums.MemberBookStatus;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.book.repository.MemberBookHistoryRepository;
import com.bookshelves.domain.book.repository.MemberBookRepository;
import com.bookshelves.domain.book.repository.MemberBookRepository.CumulativeStatistics;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository.RecentSearch;
import com.bookshelves.domain.book.util.IsbnNormalizer;
import com.bookshelves.global.security.AuthenticationFacade;
import com.bookshelves.global.util.ServiceTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookQueryService {

  private final CategoryRepository categoryRepository;
  private final BookRepository bookRepository;
  private final MemberBookRepository memberBookRepository;
  private final MemberBookHistoryRepository memberBookHistoryRepository;
  private final KakaoBookSearchClient kakaoBookSearchClient;
  private final Data4LibraryBookDetailClient data4LibraryBookDetailClient;
  private final RecentBookSearchRepository recentBookSearchRepository;
  private final AuthenticationFacade authenticationFacade;

  @Transactional(readOnly = true)
  public CategoryListResDTO getCategories() {
    try {
      List<CategoryInfo> categories =
          categoryRepository.findAllByOrderByKdcCodeAsc().stream()
              .map(this::toCategoryInfo)
              .toList();
      return new CategoryListResDTO(categories);
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.CATEGORY_LIST_FAILED);
    }
  }

  private CategoryInfo toCategoryInfo(Category category) {
    return new CategoryInfo(category.getId(), category.getKdcCode(), category.getName());
  }

  public BookSearchResDTO searchExternalBooks(
      String query, String pageValue, String sizeValue, boolean saveRecent) {
    int page = parsePageParameter(pageValue);
    int size = parsePageParameter(sizeValue);
    validateSearchRequest(query, page, size);

    String normalizedQuery = query.trim();
    KakaoBookSearchResult searchResult = kakaoBookSearchClient.search(normalizedQuery, page, size);

    List<BookInfo> books = normalizeAndDeduplicate(searchResult.books());
    if (saveRecent) {
      Long memberId = authenticationFacade.getCurrentMemberId();
      saveRecentSearchWithoutInterruptingResponse(memberId, normalizedQuery);
    }

    return new BookSearchResDTO(books, page, size, !searchResult.isEnd());
  }

  public RecentBookSearchResDTO getRecentBookSearches() {
    Long memberId = authenticationFacade.getCurrentMemberId();

    try {
      List<RecentSearchInfo> recentSearches =
          recentBookSearchRepository.findAllByMemberId(memberId).stream()
              .map(this::toRecentSearchInfo)
              .toList();
      return new RecentBookSearchResDTO(recentSearches);
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.RECENT_BOOK_SEARCHES_FAILED);
    }
  }

  @Transactional(readOnly = true)
  public MemberBookListResDTO getMemberBooks(
      String statusValue, String pageValue, String sizeValue) {
    MemberBookStatus status = parseMemberBookStatus(statusValue);
    int page = parseMemberBookListPageParameter(pageValue);
    int size = parseMemberBookListPageParameter(sizeValue);
    validateMemberBookListRequest(page, size);

    Long memberId = authenticationFacade.getCurrentMemberId();
    Pageable pageable =
        PageRequest.of(
            page - 1, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));

    try {
      Page<MemberBook> memberBooks = findMemberBooks(memberId, status, pageable);
      return new MemberBookListResDTO(
          memberBooks.getContent().stream().map(this::toMemberBookListInfo).toList(),
          page,
          size,
          memberBooks.hasNext());
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.MEMBER_BOOK_LIST_FAILED);
    }
  }

  @Transactional(readOnly = true)
  public MemberBookCalendarResDTO getMemberBookCalendar(String yearValue, String monthValue) {
    YearMonth yearMonth = parseMemberBookCalendarYearMonth(yearValue, monthValue);
    Long memberId = authenticationFacade.getCurrentMemberId();
    LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime endAt = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    try {
      List<MemberBookHistory> histories =
          memberBookHistoryRepository
              .findByMemberBookMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
                  memberId, startAt, endAt);
      return toMemberBookCalendarResDTO(yearMonth, histories);
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.MEMBER_BOOK_CALENDAR_FAILED);
    }
  }

  @Transactional(readOnly = true)
  public MemberBookStatisticsResDTO getMemberBookStatistics(String yearValue, String monthValue) {
    YearMonth yearMonth = parseMemberBookStatisticsYearMonth(yearValue, monthValue);
    Long memberId = authenticationFacade.getCurrentMemberId();
    LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
    LocalDateTime endAt = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

    try {
      CumulativeStatistics cumulativeStatistics =
          memberBookRepository.findCumulativeStatistics(memberId, 100);
      List<MemberBook> monthlyCompletedMemberBooks =
          memberBookRepository
              .findByMemberIdAndProgressAndFinishedAtGreaterThanEqualAndFinishedAtLessThan(
                  memberId, 100, startAt, endAt);
      return new MemberBookStatisticsResDTO(
          yearMonth.getYear(),
          yearMonth.getMonthValue(),
          cumulativeStatistics.getCompletedBookCount(),
          cumulativeStatistics.getReviewCount(),
          toAverageRating(cumulativeStatistics.getAverageRating()),
          calculateCategoryStatistics(monthlyCompletedMemberBooks));
    } catch (DataAccessException exception) {
      throw new BookException(BookErrorCode.MEMBER_BOOK_STATISTICS_FAILED);
    }
  }

  private YearMonth parseMemberBookStatisticsYearMonth(String yearValue, String monthValue) {
    YearMonth now = YearMonth.now(ServiceTime.ZONE);
    try {
      int year = yearValue == null ? now.getYear() : Integer.parseInt(yearValue);
      int month = monthValue == null ? now.getMonthValue() : Integer.parseInt(monthValue);
      return YearMonth.of(year, month);
    } catch (NumberFormatException | DateTimeException exception) {
      throw new BookException(BookErrorCode.INVALID_MEMBER_BOOK_STATISTICS_REQUEST);
    }
  }

  private BigDecimal toAverageRating(Double averageRating) {
    if (averageRating == null) {
      return BigDecimal.ZERO.setScale(1);
    }
    return BigDecimal.valueOf(averageRating).setScale(1, RoundingMode.DOWN);
  }

  private List<MemberBookStatisticsResDTO.CategoryStatistic> calculateCategoryStatistics(
      List<MemberBook> monthlyCompletedMemberBooks) {
    if (monthlyCompletedMemberBooks.isEmpty()) {
      return List.of();
    }

    Map<String, Long> categoryCounts = new LinkedHashMap<>();
    for (MemberBook memberBook : monthlyCompletedMemberBooks) {
      Book book = memberBook.getBook();
      String kdcCode = book.getKdcCode();
      String kdcName = book.getKdcName();
      if (kdcCode == null
          || !kdcCode.matches("\\d{3}")
          || kdcName == null
          || kdcName.isBlank()
          || kdcName.equals("미분류")) {
        continue;
      }
      categoryCounts.merge(kdcName, 1L, Long::sum);
    }
    int classifiedBookCount = categoryCounts.values().stream().mapToInt(Long::intValue).sum();
    if (classifiedBookCount == 0) {
      return List.of();
    }

    List<Map.Entry<String, Long>> sortedCategories =
        categoryCounts.entrySet().stream()
            .sorted(
                Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                    .thenComparing(Map.Entry.comparingByKey()))
            .toList();
    boolean hasMoreThanThreeCategories = sortedCategories.size() >= 4;
    List<MemberBookStatisticsResDTO.CategoryStatistic> statistics = new ArrayList<>();
    sortedCategories.stream()
        .limit(hasMoreThanThreeCategories ? 3 : sortedCategories.size())
        .forEach(
            category ->
                statistics.add(
                    toCategoryStatistic(
                        category.getKey(), category.getValue(), classifiedBookCount)));

    long otherCount =
        hasMoreThanThreeCategories
            ? sortedCategories.stream().skip(3).mapToLong(Map.Entry::getValue).sum()
            : 0;
    if (otherCount > 0) {
      statistics.add(toCategoryStatistic("기타", otherCount, classifiedBookCount));
    }
    return List.copyOf(statistics);
  }

  private MemberBookStatisticsResDTO.CategoryStatistic toCategoryStatistic(
      String name, long count, int totalCount) {
    int percentage = (int) Math.round((double) count * 100 / totalCount);
    return new MemberBookStatisticsResDTO.CategoryStatistic(name, count, percentage);
  }

  private MemberBookCalendarResDTO toMemberBookCalendarResDTO(
      YearMonth yearMonth, List<MemberBookHistory> histories) {
    Map<LocalDate, MemberBookHistory> historiesByDate = new LinkedHashMap<>();
    for (MemberBookHistory history : histories) {
      LocalDate date = history.getCreatedAt().toLocalDate();
      historiesByDate.putIfAbsent(date, history);
    }

    List<CalendarDay> days =
        historiesByDate.entrySet().stream()
            .map(
                entry ->
                    new CalendarDay(
                        entry.getKey(),
                        entry.getValue().getMemberBook().getBook().getCoverImageUrl()))
            .toList();
    return new MemberBookCalendarResDTO(yearMonth.getYear(), yearMonth.getMonthValue(), days);
  }

  private YearMonth parseMemberBookCalendarYearMonth(String yearValue, String monthValue) {
    try {
      return YearMonth.of(Integer.parseInt(yearValue), Integer.parseInt(monthValue));
    } catch (NumberFormatException | DateTimeException | NullPointerException exception) {
      throw new BookException(BookErrorCode.INVALID_MEMBER_BOOK_CALENDAR_REQUEST);
    }
  }

  private Page<MemberBook> findMemberBooks(
      Long memberId, MemberBookStatus status, Pageable pageable) {
    return switch (status) {
      case ALL -> memberBookRepository.findByMemberId(memberId, pageable);
      case BEFORE_READING -> memberBookRepository.findByMemberIdAndProgress(memberId, 0, pageable);
      case READING ->
          memberBookRepository.findByMemberIdAndProgressBetween(memberId, 1, 99, pageable);
      case FINISHED -> memberBookRepository.findByMemberIdAndProgress(memberId, 100, pageable);
    };
  }

  private MemberBookListResDTO.MemberBookInfo toMemberBookListInfo(MemberBook memberBook) {
    Book book = memberBook.getBook();

    return new MemberBookListResDTO.MemberBookInfo(
        new MemberBookRecord(
            memberBook.getId(),
            memberBook.getProgress(),
            MemberBookStatus.from(memberBook.getProgress()).name(),
            memberBook.getRating(),
            memberBook.getMemo(),
            memberBook.getUpdatedAt()),
        new MemberBookListResDTO.BookInfo(
            book.getId(),
            book.getIsbn(),
            book.getTitle(),
            book.getAuthor(),
            book.getPublisher(),
            book.getCoverImageUrl(),
            book.getKdcCode(),
            book.getKdcName()));
  }

  private MemberBookStatus parseMemberBookStatus(String value) {
    try {
      return MemberBookStatus.valueOf(value);
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BookException(BookErrorCode.INVALID_MEMBER_BOOK_LIST_REQUEST);
    }
  }

  private int parseMemberBookListPageParameter(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException | NullPointerException exception) {
      throw new BookException(BookErrorCode.INVALID_MEMBER_BOOK_LIST_REQUEST);
    }
  }

  private void validateMemberBookListRequest(int page, int size) {
    if (page < 1 || size < 1 || size > 50) {
      throw new BookException(BookErrorCode.INVALID_MEMBER_BOOK_LIST_REQUEST);
    }
  }

  public BookDetailResDTO getBookDetail(String rawIsbn) {
    String requestedIsbn =
        IsbnNormalizer.normalize(rawIsbn)
            .orElseThrow(() -> new BookException(BookErrorCode.INVALID_BOOK_ISBN));
    String canonicalIsbn = IsbnNormalizer.toIsbn13(requestedIsbn);
    Long memberId = authenticationFacade.getCurrentMemberId();

    Book savedBook = bookRepository.findByIsbn(canonicalIsbn).orElse(null);
    MemberBook memberBook =
        savedBook == null
            ? null
            : memberBookRepository
                .findByMemberIdAndBookId(memberId, savedBook.getId())
                .orElse(null);

    if (savedBook != null) {
      return new BookDetailResDTO(toSavedBookDetailInfo(savedBook), toMemberBookInfo(memberBook));
    }

    KakaoBookItem externalBook =
        kakaoBookSearchClient.searchByIsbn(requestedIsbn).books().stream()
            .findFirst()
            .orElseThrow(() -> new BookException(BookErrorCode.BOOK_NOT_FOUND));
    String externalIsbn = IsbnNormalizer.normalize(externalBook.isbn()).orElse(requestedIsbn);
    String canonicalExternalIsbn = IsbnNormalizer.toIsbn13(externalIsbn);
    KdcInfo kdcInfo = data4LibraryBookDetailClient.findKdcByIsbn(canonicalExternalIsbn);

    return new BookDetailResDTO(
        toExternalBookDetailInfo(externalBook, canonicalExternalIsbn, kdcInfo), null);
  }

  private BookDetailResDTO.BookInfo toExternalBookDetailInfo(
      KakaoBookItem item, String isbn, KdcInfo kdcInfo) {
    return new BookDetailResDTO.BookInfo(
        null,
        isbn,
        item.title(),
        toAuthor(item),
        item.publisher(),
        parsePublishedDate(item.datetime()),
        item.contents(),
        item.thumbnail(),
        kdcInfo.code(),
        kdcInfo.name());
  }

  private BookDetailResDTO.BookInfo toSavedBookDetailInfo(Book book) {
    return new BookDetailResDTO.BookInfo(
        book.getId(),
        book.getIsbn(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublisher(),
        book.getPublishedDate(),
        book.getDescription(),
        book.getCoverImageUrl(),
        book.getKdcCode(),
        book.getKdcName());
  }

  private MemberBookInfo toMemberBookInfo(MemberBook memberBook) {
    if (memberBook == null) {
      return null;
    }
    return new MemberBookInfo(
        memberBook.getId(), memberBook.getProgress(), memberBook.getRating(), memberBook.getMemo());
  }

  private RecentSearchInfo toRecentSearchInfo(RecentSearch recentSearch) {
    return new RecentSearchInfo(
        recentSearch.keyword(),
        Instant.ofEpochMilli(recentSearch.searchedAtEpochMillis())
            .atZone(ServiceTime.ZONE)
            .toOffsetDateTime());
  }

  private int parsePageParameter(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException | NullPointerException exception) {
      throw new BookException(BookErrorCode.INVALID_BOOK_SEARCH_REQUEST);
    }
  }

  private void validateSearchRequest(String query, int page, int size) {
    if (query == null || query.isBlank() || page < 1 || page > 50 || size < 1 || size > 50) {
      throw new BookException(BookErrorCode.INVALID_BOOK_SEARCH_REQUEST);
    }
  }

  private List<BookInfo> normalizeAndDeduplicate(List<KakaoBookItem> items) {
    List<BookInfo> books = new ArrayList<>();
    Set<String> seenIsbns = new HashSet<>();

    for (KakaoBookItem item : items) {
      String isbn = IsbnNormalizer.normalize(item.isbn()).orElse(null);
      if (isbn != null && !seenIsbns.add(isbn)) {
        continue;
      }
      books.add(toBookInfo(item, isbn));
    }
    return List.copyOf(books);
  }

  private BookInfo toBookInfo(KakaoBookItem item, String isbn) {
    return new BookInfo(
        isbn,
        item.title(),
        toAuthor(item),
        item.publisher(),
        parsePublishedDate(item.datetime()),
        item.contents(),
        item.thumbnail(),
        isbn != null);
  }

  private String toAuthor(KakaoBookItem item) {
    return item.authors() == null || item.authors().isEmpty()
        ? null
        : String.join(", ", item.authors());
  }

  private LocalDate parsePublishedDate(String datetime) {
    if (datetime == null || datetime.length() < 10) {
      return null;
    }

    try {
      return LocalDate.parse(datetime.substring(0, 10));
    } catch (DateTimeParseException exception) {
      return null;
    }
  }

  private void saveRecentSearchWithoutInterruptingResponse(Long memberId, String query) {
    try {
      recentBookSearchRepository.save(memberId, query);
    } catch (RuntimeException exception) {
      log.warn("최근 도서 검색어 저장에 실패했습니다. memberId={}", memberId, exception);
    }
  }
}
