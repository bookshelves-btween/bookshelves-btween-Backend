package com.bookshelves.domain.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient;
import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.ExternalBookCacheRepository;
import com.bookshelves.domain.book.repository.ExternalBookCacheRepository.CachedBookDetail;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalBookLookupServiceTest {

  private static final String ISBN_13 = "9788936434595";

  @Mock private KakaoBookSearchClient kakaoBookSearchClient;
  @Mock private Data4LibraryBookDetailClient data4LibraryBookDetailClient;
  @Mock private ExternalBookCacheRepository externalBookCacheRepository;
  @InjectMocks private ExternalBookLookupService externalBookLookupService;

  @Test
  void searchReturnsSharedCacheWithoutCallingExternalApi() {
    KakaoBookSearchResult cached = new KakaoBookSearchResult(List.of(), true);
    given(externalBookCacheRepository.findSearch("혼모노", 1, 15)).willReturn(Optional.of(cached));

    KakaoBookSearchResult result = externalBookLookupService.search("혼모노", 1, 15);

    assertThat(result).isSameAs(cached);
    verifyNoInteractions(kakaoBookSearchClient, data4LibraryBookDetailClient);
  }

  @Test
  void searchCachesSuccessfulExternalResponseIncludingEmptyResult() {
    KakaoBookSearchResult response = new KakaoBookSearchResult(List.of(), true);
    given(externalBookCacheRepository.findSearch("혼모노", 1, 15)).willReturn(Optional.empty());
    given(kakaoBookSearchClient.search("혼모노", 1, 15)).willReturn(response);

    KakaoBookSearchResult result = externalBookLookupService.search("혼모노", 1, 15);

    assertThat(result).isSameAs(response);
    verify(externalBookCacheRepository).saveSearch("혼모노", 1, 15, response);
  }

  @Test
  void detailReturnsSharedCacheWithoutCallingExternalApi() {
    CachedBookDetail cached =
        new CachedBookDetail(bookItem(ISBN_13), ISBN_13, new KdcInfo("813", "문학"));
    given(externalBookCacheRepository.findDetail(ISBN_13)).willReturn(Optional.of(cached));

    CachedBookDetail result = externalBookLookupService.findByIsbn(ISBN_13, ISBN_13);

    assertThat(result).isSameAs(cached);
    verifyNoInteractions(kakaoBookSearchClient, data4LibraryBookDetailClient);
  }

  @Test
  void detailNormalizesExternalIsbnAndCachesKakaoAndKdcTogether() {
    KakaoBookItem item = bookItem("8936434594");
    KdcInfo kdcInfo = new KdcInfo("813", "문학");
    given(externalBookCacheRepository.findDetail(ISBN_13)).willReturn(Optional.empty());
    given(kakaoBookSearchClient.searchByIsbn("8936434594"))
        .willReturn(new KakaoBookSearchResult(List.of(item), true));
    given(data4LibraryBookDetailClient.findKdcByIsbn(ISBN_13)).willReturn(kdcInfo);

    CachedBookDetail result = externalBookLookupService.findByIsbn("8936434594", ISBN_13);

    assertThat(result.canonicalIsbn()).isEqualTo(ISBN_13);
    assertThat(result.kdcInfo()).isEqualTo(kdcInfo);
    verify(externalBookCacheRepository).saveDetail(ISBN_13, result);
  }

  @Test
  void detailDoesNotCacheWhenExternalBookDoesNotExist() {
    given(externalBookCacheRepository.findDetail(ISBN_13)).willReturn(Optional.empty());
    given(kakaoBookSearchClient.searchByIsbn(ISBN_13))
        .willReturn(new KakaoBookSearchResult(List.of(), true));

    assertThatThrownBy(() -> externalBookLookupService.findByIsbn(ISBN_13, ISBN_13))
        .isInstanceOf(BookException.class)
        .extracting(exception -> ((BookException) exception).getErrorCode())
        .isEqualTo(BookErrorCode.BOOK_NOT_FOUND);

    verify(externalBookCacheRepository, never())
        .saveDetail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    verifyNoInteractions(data4LibraryBookDetailClient);
  }

  private KakaoBookItem bookItem(String isbn) {
    return new KakaoBookItem(isbn, "아몬드", List.of("손원평"), "창비", null, null, null);
  }
}
