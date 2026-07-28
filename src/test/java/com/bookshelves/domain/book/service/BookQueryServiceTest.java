package com.bookshelves.domain.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient;
import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.dto.response.BookDetailResDTO;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
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
import com.bookshelves.global.security.AuthenticationFacade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class BookQueryServiceTest {

  @Mock private CategoryRepository categoryRepository;
  @Mock private BookRepository bookRepository;
  @Mock private MemberBookRepository memberBookRepository;
  @Mock private KakaoBookSearchClient kakaoBookSearchClient;
  @Mock private Data4LibraryBookDetailClient data4LibraryBookDetailClient;
  @Mock private RecentBookSearchRepository recentBookSearchRepository;
  @Mock private AuthenticationFacade authenticationFacade;
  @InjectMocks private BookQueryService bookQueryService;

  @Test
  void getCategoriesReturnsCategoriesInRepositoryOrder() {
    Category generalities = category(1L, "000", "총류");
    Category literature = category(9L, "800", "문학");
    given(categoryRepository.findAllByOrderByKdcCodeAsc())
        .willReturn(List.of(generalities, literature));

    CategoryListResDTO result = bookQueryService.getCategories();

    assertThat(result.categories()).hasSize(2);
    assertThat(result.categories().getFirst().id()).isEqualTo(1L);
    assertThat(result.categories().getFirst().kdcCode()).isEqualTo("000");
    assertThat(result.categories().getFirst().name()).isEqualTo("총류");
    assertThat(result.categories().getLast().kdcCode()).isEqualTo("800");
  }

  @Test
  void getCategoriesThrowsBookExceptionWhenDatabaseAccessFails() {
    given(categoryRepository.findAllByOrderByKdcCodeAsc())
        .willThrow(new DataAccessResourceFailureException("database unavailable"));

    assertThatThrownBy(bookQueryService::getCategories)
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.CATEGORY_LIST_FAILED));
  }

  @Test
  void searchExternalBooksNormalizesDeduplicatesAndSavesRecentQuery() {
    KakaoBookItem first =
        new KakaoBookItem(
            "8996991341 9788996991342",
            "미움받을 용기",
            List.of("기시미 이치로", "고가 후미타케"),
            "인플루엔셜",
            "2014-11-17T00:00:00.000+09:00",
            "도서 소개",
            "https://example.com/cover.jpg");
    KakaoBookItem duplicate =
        new KakaoBookItem(
            "9788996991342",
            "미움받을 용기 중복",
            List.of("기시미 이치로"),
            "인플루엔셜",
            "2014-11-17T00:00:00.000+09:00",
            "중복",
            "https://example.com/duplicate.jpg");
    KakaoBookItem noIsbn =
        new KakaoBookItem(
            "",
            "ISBN 없는 책",
            List.of(),
            "출판사",
            "invalid-date",
            "소개",
            "https://example.com/no-isbn.jpg");

    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(kakaoBookSearchClient.search("미움받을 용기", 1, 15))
        .willReturn(new KakaoBookSearchResult(List.of(first, duplicate, noIsbn), false));

    BookSearchResDTO result = bookQueryService.searchExternalBooks("  미움받을 용기  ", "1", "15", true);

    assertThat(result.books()).hasSize(2);
    assertThat(result.books().getFirst().isbn()).isEqualTo("9788996991342");
    assertThat(result.books().getFirst().author()).isEqualTo("기시미 이치로, 고가 후미타케");
    assertThat(result.books().getFirst().publishedDate()).isEqualTo(LocalDate.of(2014, 11, 17));
    assertThat(result.books().getFirst().saveable()).isTrue();
    assertThat(result.books().getLast().isbn()).isNull();
    assertThat(result.books().getLast().publishedDate()).isNull();
    assertThat(result.books().getLast().saveable()).isFalse();
    assertThat(result.hasNext()).isTrue();
    verify(recentBookSearchRepository).save(7L, "미움받을 용기");
  }

  @Test
  void searchExternalBooksRejectsInvalidRequestBeforeAuthenticationAndExternalCall() {
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks(" ", "1", "15", true))
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.INVALID_BOOK_SEARCH_REQUEST));

    verifyNoInteractions(authenticationFacade, kakaoBookSearchClient, recentBookSearchRepository);
  }

  @Test
  void searchExternalBooksRejectsPageAndSizeOutsideSupportedRange() {
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks("책", "0", "15", true))
        .isInstanceOf(BookException.class);
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks("책", "1", "51", true))
        .isInstanceOf(BookException.class);
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks("책", "abc", "15", true))
        .isInstanceOf(BookException.class);

    verifyNoInteractions(authenticationFacade, kakaoBookSearchClient, recentBookSearchRepository);
  }

  @Test
  void searchExternalBooksReturnsResultEvenWhenRecentSearchSaveFails() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(kakaoBookSearchClient.search("책", 1, 15))
        .willReturn(new KakaoBookSearchResult(List.of(), true));
    doThrow(new RuntimeException("redis unavailable"))
        .when(recentBookSearchRepository)
        .save(7L, "책");

    BookSearchResDTO result = bookQueryService.searchExternalBooks("책", "1", "15", true);

    assertThat(result.books()).isEmpty();
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  void searchExternalBooksDoesNotSaveRecentQueryWhenSaveRecentIsFalse() {
    given(kakaoBookSearchClient.search("혼모노", 1, 15))
        .willReturn(new KakaoBookSearchResult(List.of(), true));

    BookSearchResDTO result = bookQueryService.searchExternalBooks("혼모노", "1", "15", false);

    assertThat(result.books()).isEmpty();
    assertThat(result.hasNext()).isFalse();
    verifyNoInteractions(authenticationFacade, recentBookSearchRepository);
  }

  @Test
  void getRecentBookSearchesReturnsSearchesInRepositoryOrderWithSeoulOffset() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(recentBookSearchRepository.findAllByMemberId(7L))
        .willReturn(
            List.of(
                new RecentSearch("혼모노", 1_721_000_000_000L),
                new RecentSearch("미움받을 용기", 1_720_000_000_000L)));

    RecentBookSearchResDTO result = bookQueryService.getRecentBookSearches();

    assertThat(result.recentSearches())
        .extracting(RecentBookSearchResDTO.RecentSearchInfo::keyword)
        .containsExactly("혼모노", "미움받을 용기");
    assertThat(result.recentSearches().getFirst().searchedAt())
        .isEqualTo(OffsetDateTime.parse("2024-07-15T08:33:20+09:00"));
  }

  @Test
  void getRecentBookSearchesReturnsEmptyListWhenMemberHasNoRecentSearch() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(recentBookSearchRepository.findAllByMemberId(7L)).willReturn(List.of());

    RecentBookSearchResDTO result = bookQueryService.getRecentBookSearches();

    assertThat(result.recentSearches()).isEmpty();
  }

  @Test
  void getRecentBookSearchesThrowsBookExceptionWhenRedisLookupFails() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(recentBookSearchRepository.findAllByMemberId(7L))
        .willThrow(new DataAccessResourceFailureException("redis unavailable"));

    assertThatThrownBy(bookQueryService::getRecentBookSearches)
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.RECENT_BOOK_SEARCHES_FAILED));
  }

  @Test
  void getBookDetailReturnsExternalBookAndMemberBookWhenSaved() {
    KakaoBookItem externalBook =
        new KakaoBookItem(
            "9788936434595",
            "혼모노",
            List.of("성해나"),
            "창비",
            "2024-03-29T00:00:00.000+09:00",
            "도서 설명입니다.",
            "https://example.com/book.jpg");
    Book savedBook = mock(Book.class);
    MemberBook memberBook = mock(MemberBook.class);

    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(bookRepository.findByIsbn("9788936434595")).willReturn(Optional.of(savedBook));
    given(savedBook.getId()).willReturn(10L);
    given(savedBook.getIsbn()).willReturn("9788936434595");
    given(savedBook.getTitle()).willReturn("혼모노");
    given(savedBook.getAuthor()).willReturn("성해나");
    given(savedBook.getPublisher()).willReturn("창비");
    given(savedBook.getPublishedDate()).willReturn(LocalDate.of(2024, 3, 29));
    given(savedBook.getDescription()).willReturn("a".repeat(127));
    given(savedBook.getCoverImageUrl()).willReturn("https://example.com/book.jpg");
    given(savedBook.getKdcCode()).willReturn("813");
    given(savedBook.getKdcName()).willReturn("문학");
    given(memberBookRepository.findByMemberIdAndBookId(7L, 10L))
        .willReturn(Optional.of(memberBook));
    given(memberBook.getId()).willReturn(20L);
    given(memberBook.getProgress()).willReturn(70);
    given(memberBook.getRating()).willReturn(BigDecimal.valueOf(4.5));
    given(memberBook.getMemo()).willReturn("진짜란 무엇인가?");

    BookDetailResDTO result = bookQueryService.getBookDetail("9788936434595");

    assertThat(result.book().id()).isEqualTo(10L);
    assertThat(result.book().kdcCode()).isEqualTo("813");
    assertThat(result.book().kdcName()).isEqualTo("문학");
    assertThat(result.book().description()).hasSize(129).isEqualTo("a".repeat(126) + "...");
    assertThat(result.memberBook().progress()).isEqualTo(70);
    assertThat(result.memberBook().rating()).isEqualByComparingTo("4.5");
    verifyNoInteractions(kakaoBookSearchClient, data4LibraryBookDetailClient);
  }

  @Test
  void getBookDetailReturnsNullMemberBookWhenSavedBookHasNoReadingRecord() {
    Book savedBook = mock(Book.class);

    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(bookRepository.findByIsbn("9788936434595")).willReturn(Optional.of(savedBook));
    given(savedBook.getId()).willReturn(10L);
    given(savedBook.getIsbn()).willReturn("9788936434595");
    given(savedBook.getTitle()).willReturn("혼모노");
    given(savedBook.getKdcName()).willReturn("문학");
    given(memberBookRepository.findByMemberIdAndBookId(7L, 10L)).willReturn(Optional.empty());

    BookDetailResDTO result = bookQueryService.getBookDetail("9788936434595");

    assertThat(result.book().id()).isEqualTo(10L);
    assertThat(result.book().title()).isEqualTo("혼모노");
    assertThat(result.memberBook()).isNull();
    verifyNoInteractions(kakaoBookSearchClient, data4LibraryBookDetailClient);
  }

  @Test
  void getBookDetailFindsSavedBookWhenRequestedWithEquivalentIsbn10() {
    Book savedBook = mock(Book.class);

    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(bookRepository.findByIsbn("9788936434595")).willReturn(Optional.of(savedBook));
    given(savedBook.getId()).willReturn(10L);
    given(savedBook.getIsbn()).willReturn("9788936434595");
    given(savedBook.getTitle()).willReturn("아몬드");
    given(savedBook.getKdcName()).willReturn("문학");
    given(memberBookRepository.findByMemberIdAndBookId(7L, 10L)).willReturn(Optional.empty());

    BookDetailResDTO result = bookQueryService.getBookDetail("8936434594");

    assertThat(result.book().id()).isEqualTo(10L);
    assertThat(result.memberBook()).isNull();
    verifyNoInteractions(kakaoBookSearchClient, data4LibraryBookDetailClient);
  }

  @Test
  void getBookDetailReturnsNullMemberBookAndUnclassifiedWhenBookIsNotSaved() {
    KakaoBookItem externalBook =
        new KakaoBookItem(
            "9788936434595",
            "혼모노",
            List.of("성해나"),
            "창비",
            "2024-03-29T00:00:00.000+09:00",
            "a".repeat(127),
            "https://example.com/book.jpg");

    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(kakaoBookSearchClient.searchByIsbn("9788936434595"))
        .willReturn(new KakaoBookSearchResult(List.of(externalBook), true));
    given(bookRepository.findByIsbn("9788936434595")).willReturn(Optional.empty());
    given(data4LibraryBookDetailClient.findKdcByIsbn("9788936434595"))
        .willReturn(new KdcInfo("813", "문학"));

    BookDetailResDTO result = bookQueryService.getBookDetail("9788936434595");

    assertThat(result.book().id()).isNull();
    assertThat(result.book().kdcCode()).isEqualTo("813");
    assertThat(result.book().kdcName()).isEqualTo("문학");
    assertThat(result.book().description()).hasSize(129).isEqualTo("a".repeat(126) + "...");
    assertThat(result.memberBook()).isNull();
    verifyNoInteractions(memberBookRepository);
  }

  @Test
  void getBookDetailRejectsInvalidIsbnBeforeAuthenticationAndExternalCall() {
    assertThatThrownBy(() -> bookQueryService.getBookDetail("invalid-isbn"))
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.INVALID_BOOK_ISBN));

    verifyNoInteractions(
        authenticationFacade,
        kakaoBookSearchClient,
        data4LibraryBookDetailClient,
        bookRepository,
        memberBookRepository);
  }

  @Test
  void getBookDetailThrowsWhenExternalBookDoesNotExist() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(7L);
    given(bookRepository.findByIsbn("9788936434595")).willReturn(Optional.empty());
    given(kakaoBookSearchClient.searchByIsbn("9788936434595"))
        .willReturn(new KakaoBookSearchResult(List.of(), true));

    assertThatThrownBy(() -> bookQueryService.getBookDetail("9788936434595"))
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.BOOK_NOT_FOUND));
  }

  private Category category(Long id, String kdcCode, String name) {
    Category category = mock(Category.class);
    given(category.getId()).willReturn(id);
    given(category.getKdcCode()).willReturn(kdcCode);
    given(category.getName()).willReturn(name);
    return category;
  }
}
