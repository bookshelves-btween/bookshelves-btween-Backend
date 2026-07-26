package com.bookshelves.domain.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient;
import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookSearchResult;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookCommandServiceTest {

  private static final String ISBN = "9788936434595";

  @Mock private BookRepository bookRepository;
  @Mock private KakaoBookSearchClient kakaoBookSearchClient;
  @Mock private Data4LibraryBookDetailClient data4LibraryBookDetailClient;
  @InjectMocks private BookCommandService bookCommandService;

  @Test
  void returnsSavedBookWithoutCallingKakaoApi() {
    Book savedBook = Book.builder().isbn(ISBN).title("아몬드").build();
    given(bookRepository.findByIsbn(ISBN)).willReturn(Optional.of(savedBook));

    Book result = bookCommandService.getOrCreateByIsbn(ISBN);

    assertThat(result).isSameAs(savedBook);
    verify(kakaoBookSearchClient, never()).searchByIsbn(ISBN);
    verify(data4LibraryBookDetailClient, never()).findKdcByIsbn(ISBN);
  }

  @Test
  void savesBookFromKakaoApiWhenBookDoesNotExist() {
    KakaoBookItem item =
        new KakaoBookItem(
            "9788936434595 8936434598",
            "아몬드",
            List.of("손원평"),
            "창비",
            "2017-03-31T00:00:00.000+09:00",
            "책 소개",
            "https://image.example.com/almond.jpg");
    given(bookRepository.findByIsbn(ISBN)).willReturn(Optional.empty());
    given(kakaoBookSearchClient.searchByIsbn(ISBN))
        .willReturn(new KakaoBookSearchResult(List.of(item), true));
    given(data4LibraryBookDetailClient.findKdcByIsbn(ISBN)).willReturn(new KdcInfo("813", "문학"));
    Book savedBook =
        Book.builder()
            .isbn(ISBN)
            .title("아몬드")
            .author("손원평")
            .publisher("창비")
            .publishedDate(LocalDate.of(2017, 3, 31))
            .description("책 소개")
            .coverImageUrl("https://image.example.com/almond.jpg")
            .kdcCode("813")
            .kdcName("문학")
            .build();
    given(bookRepository.findByIsbnForUpdate(ISBN)).willReturn(Optional.of(savedBook));

    Book result = bookCommandService.getOrCreateByIsbn(ISBN);

    verify(bookRepository)
        .upsert(
            ISBN,
            "아몬드",
            "손원평",
            "창비",
            LocalDate.of(2017, 3, 31),
            "책 소개",
            "https://image.example.com/almond.jpg",
            "813",
            "문학");
    assertThat(result).isSameAs(savedBook);
    assertThat(result.getIsbn()).isEqualTo(ISBN);
    assertThat(result.getTitle()).isEqualTo("아몬드");
    assertThat(result.getAuthor()).isEqualTo("손원평");
    assertThat(result.getPublisher()).isEqualTo("창비");
    assertThat(result.getPublishedDate()).isEqualTo(LocalDate.of(2017, 3, 31));
    assertThat(result.getDescription()).isEqualTo("책 소개");
    assertThat(result.getCoverImageUrl()).isEqualTo("https://image.example.com/almond.jpg");
    assertThat(result.getKdcCode()).isEqualTo("813");
    assertThat(result.getKdcName()).isEqualTo("문학");
  }

  @Test
  void throwsBookNotFoundWhenKakaoApiReturnsNoBook() {
    given(bookRepository.findByIsbn(ISBN)).willReturn(Optional.empty());
    given(kakaoBookSearchClient.searchByIsbn(ISBN))
        .willReturn(new KakaoBookSearchResult(List.of(), true));

    assertThatThrownBy(() -> bookCommandService.getOrCreateByIsbn(ISBN))
        .isInstanceOf(BookException.class)
        .extracting(exception -> ((BookException) exception).getErrorCode())
        .isEqualTo(BookErrorCode.BOOK_NOT_FOUND);

    verify(bookRepository, never())
        .upsert(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    verify(data4LibraryBookDetailClient, never()).findKdcByIsbn(ISBN);
  }

  @Test
  void concurrentCreationReloadsBookSavedByWinningRequest() throws Exception {
    KakaoBookItem item = new KakaoBookItem(ISBN, "아몬드", List.of("손원평"), "창비", null, null, null);
    Book winningBook = Book.builder().isbn(ISBN).title("아몬드").build();
    CountDownLatch bothRequestsReachedLookup = new CountDownLatch(2);
    CountDownLatch releaseLookup = new CountDownLatch(1);

    given(bookRepository.findByIsbn(ISBN))
        .willAnswer(
            invocation -> {
              bothRequestsReachedLookup.countDown();
              releaseLookup.await(3, TimeUnit.SECONDS);
              return Optional.empty();
            });
    given(kakaoBookSearchClient.searchByIsbn(ISBN))
        .willReturn(new KakaoBookSearchResult(List.of(item), true));
    given(data4LibraryBookDetailClient.findKdcByIsbn(ISBN)).willReturn(new KdcInfo(null, "미분류"));
    given(bookRepository.findByIsbnForUpdate(ISBN)).willReturn(Optional.of(winningBook));

    CompletableFuture<Book> first =
        CompletableFuture.supplyAsync(() -> bookCommandService.getOrCreateByIsbn(ISBN));
    CompletableFuture<Book> second =
        CompletableFuture.supplyAsync(() -> bookCommandService.getOrCreateByIsbn(ISBN));

    assertThat(bothRequestsReachedLookup.await(3, TimeUnit.SECONDS)).isTrue();
    releaseLookup.countDown();

    assertThat(first.get(3, TimeUnit.SECONDS)).isSameAs(winningBook);
    assertThat(second.get(3, TimeUnit.SECONDS)).isSameAs(winningBook);
    verify(bookRepository, times(2)).findByIsbnForUpdate(ISBN);
  }
}
