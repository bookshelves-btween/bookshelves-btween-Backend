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
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO.RecentSearchInfo;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.entity.MemberBook;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.book.repository.MemberBookRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository.RecentSearch;
import com.bookshelves.domain.book.util.IsbnNormalizer;
import com.bookshelves.global.security.AuthenticationFacade;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookQueryService {

  private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
  private static final int DESCRIPTION_PREVIEW_LENGTH = 126;
  private static final String DESCRIPTION_SUFFIX = "...";

  private final CategoryRepository categoryRepository;
  private final BookRepository bookRepository;
  private final MemberBookRepository memberBookRepository;
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

  public BookSearchResDTO searchExternalBooks(String query, String pageValue, String sizeValue) {
    int page = parsePageParameter(pageValue);
    int size = parsePageParameter(sizeValue);
    validateSearchRequest(query, page, size);

    String normalizedQuery = query.trim();
    Long memberId = authenticationFacade.getCurrentMemberId();
    KakaoBookSearchResult searchResult = kakaoBookSearchClient.search(normalizedQuery, page, size);

    List<BookInfo> books = normalizeAndDeduplicate(searchResult.books());
    saveRecentSearchWithoutInterruptingResponse(memberId, normalizedQuery);

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
    String normalizedIsbn = IsbnNormalizer.normalize(externalBook.isbn()).orElse(requestedIsbn);
    KdcInfo kdcInfo = data4LibraryBookDetailClient.findKdcByIsbn(normalizedIsbn);

    return new BookDetailResDTO(
        toExternalBookDetailInfo(externalBook, normalizedIsbn, kdcInfo), null);
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
        truncateDescription(item.contents()),
        item.thumbnail(),
        kdcInfo.code(),
        kdcInfo.name());
  }

  private BookDetailResDTO.BookInfo toSavedBookDetailInfo(Book book) {
    String kdcName = book.getKdcName();
    if (kdcName == null || kdcName.isBlank()) {
      kdcName = "미분류";
    }

    return new BookDetailResDTO.BookInfo(
        book.getId(),
        book.getIsbn(),
        book.getTitle(),
        book.getAuthor(),
        book.getPublisher(),
        book.getPublishedDate(),
        truncateDescription(book.getDescription()),
        book.getCoverImageUrl(),
        book.getKdcCode(),
        kdcName);
  }

  private MemberBookInfo toMemberBookInfo(MemberBook memberBook) {
    if (memberBook == null) {
      return null;
    }
    return new MemberBookInfo(
        memberBook.getId(), memberBook.getProgress(), memberBook.getRating(), memberBook.getMemo());
  }

  private String truncateDescription(String description) {
    if (description == null
        || description.codePointCount(0, description.length()) <= DESCRIPTION_PREVIEW_LENGTH) {
      return description;
    }

    int endIndex = description.offsetByCodePoints(0, DESCRIPTION_PREVIEW_LENGTH);
    return description.substring(0, endIndex) + DESCRIPTION_SUFFIX;
  }

  private RecentSearchInfo toRecentSearchInfo(RecentSearch recentSearch) {
    return new RecentSearchInfo(
        recentSearch.keyword(),
        Instant.ofEpochMilli(recentSearch.searchedAtEpochMillis())
            .atZone(SEOUL_ZONE_ID)
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
