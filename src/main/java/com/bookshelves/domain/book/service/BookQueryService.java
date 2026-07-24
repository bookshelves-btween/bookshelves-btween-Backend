package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO.BookInfo;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO.CategoryInfo;
import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository;
import com.bookshelves.domain.book.util.IsbnNormalizer;
import com.bookshelves.global.security.AuthenticationFacade;
import java.time.LocalDate;
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

  private final CategoryRepository categoryRepository;
  private final KakaoBookSearchClient kakaoBookSearchClient;
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
    String author =
        item.authors() == null || item.authors().isEmpty()
            ? null
            : String.join(", ", item.authors());
    return new BookInfo(
        isbn,
        item.title(),
        author,
        item.publisher(),
        parsePublishedDate(item.datetime()),
        item.contents(),
        item.thumbnail(),
        isbn != null);
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
