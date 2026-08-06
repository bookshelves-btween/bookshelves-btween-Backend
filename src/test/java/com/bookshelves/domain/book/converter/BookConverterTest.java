package com.bookshelves.domain.book.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.entity.Book;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookConverterTest {

  @Test
  void truncatesExternalTitleAndAuthorByCodePoint() {
    String overlongValue = "😀".repeat(256);
    KakaoBookItem item =
        new KakaoBookItem(
            "9788936434595",
            overlongValue,
            List.of(overlongValue),
            "창비",
            "2024-03-29T00:00:00.000+09:00",
            "도서 설명",
            "https://example.com/book.jpg");

    Book book = BookConverter.toEntity(item, "9788936434595", new KdcInfo("813", "문학"));

    assertThat(book.getTitle()).isEqualTo("😀".repeat(Book.MAX_TITLE_LENGTH));
    assertThat(book.getAuthor()).isEqualTo("😀".repeat(Book.MAX_AUTHOR_LENGTH));
    assertThat(book.getTitle().codePointCount(0, book.getTitle().length()))
        .isEqualTo(Book.MAX_TITLE_LENGTH);
    assertThat(book.getAuthor().codePointCount(0, book.getAuthor().length()))
        .isEqualTo(Book.MAX_AUTHOR_LENGTH);
  }

  @Test
  void storesBothKdcFieldsAsNullWhenKdcCodeIsUnavailable() {
    KakaoBookItem item =
        new KakaoBookItem("9788936434595", "혼모노", List.of("성해나"), "창비", null, null, null);

    Book bookWithMissingCode =
        BookConverter.toEntity(item, "9788936434595", new KdcInfo(null, "미분류"));
    Book bookWithMissingName =
        BookConverter.toEntity(item, "9788936434595", new KdcInfo("813", null));

    assertThat(bookWithMissingCode.getKdcCode()).isNull();
    assertThat(bookWithMissingCode.getKdcName()).isNull();
    assertThat(bookWithMissingName.getKdcCode()).isNull();
    assertThat(bookWithMissingName.getKdcName()).isNull();
  }
}
