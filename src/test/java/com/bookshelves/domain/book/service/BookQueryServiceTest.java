package com.bookshelves.domain.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.dto.response.BookSearchResDTO;
import com.bookshelves.domain.book.dto.response.CategoryListResDTO;
import com.bookshelves.domain.book.dto.response.RecentBookSearchResDTO;
import com.bookshelves.domain.book.entity.Category;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.CategoryRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository;
import com.bookshelves.domain.book.repository.RecentBookSearchRepository.RecentSearch;
import com.bookshelves.global.security.AuthenticationFacade;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class BookQueryServiceTest {

  @Mock private CategoryRepository categoryRepository;
  @Mock private KakaoBookSearchClient kakaoBookSearchClient;
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

    BookSearchResDTO result = bookQueryService.searchExternalBooks("  미움받을 용기  ", "1", "15");

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
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks(" ", "1", "15"))
        .isInstanceOf(BookException.class)
        .satisfies(
            exception ->
                assertThat(((BookException) exception).getErrorCode())
                    .isEqualTo(BookErrorCode.INVALID_BOOK_SEARCH_REQUEST));

    verifyNoInteractions(authenticationFacade, kakaoBookSearchClient, recentBookSearchRepository);
  }

  @Test
  void searchExternalBooksRejectsPageAndSizeOutsideSupportedRange() {
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks("책", "0", "15"))
        .isInstanceOf(BookException.class);
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks("책", "1", "51"))
        .isInstanceOf(BookException.class);
    assertThatThrownBy(() -> bookQueryService.searchExternalBooks("책", "abc", "15"))
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

    BookSearchResDTO result = bookQueryService.searchExternalBooks("책", "1", "15");

    assertThat(result.books()).isEmpty();
    assertThat(result.hasNext()).isFalse();
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

  private Category category(Long id, String kdcCode, String name) {
    Category category = mock(Category.class);
    given(category.getId()).willReturn(id);
    given(category.getKdcCode()).willReturn(kdcCode);
    given(category.getName()).willReturn(name);
    return category;
  }
}
