package com.bookshelves.domain.book.converter;

import com.bookshelves.domain.book.client.Data4LibraryBookDetailClient.KdcInfo;
import com.bookshelves.domain.book.client.KakaoBookSearchClient.KakaoBookItem;
import com.bookshelves.domain.book.entity.Book;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class BookConverter {

  private BookConverter() {}

  public static Book toEntity(KakaoBookItem item, String isbn, KdcInfo kdcInfo) {
    return Book.builder()
        .isbn(isbn)
        .title(item.title())
        .author(toAuthor(item))
        .publisher(item.publisher())
        .publishedDate(parsePublishedDate(item.datetime()))
        .description(item.contents())
        .coverImageUrl(item.thumbnail())
        .kdcCode(kdcInfo.code())
        .kdcName(kdcInfo.name())
        .build();
  }

  private static String toAuthor(KakaoBookItem item) {
    return item.authors() == null || item.authors().isEmpty()
        ? null
        : String.join(", ", item.authors());
  }

  private static LocalDate parsePublishedDate(String datetime) {
    if (datetime == null || datetime.length() < 10) {
      return null;
    }

    try {
      return LocalDate.parse(datetime.substring(0, 10));
    } catch (DateTimeParseException exception) {
      return null;
    }
  }
}
