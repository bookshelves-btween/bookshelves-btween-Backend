package com.bookshelves.domain.book.dto.response;

import java.time.LocalDate;
import java.util.List;

public record BookSearchResDTO(List<BookInfo> books, Integer page, Integer size, boolean hasNext) {

  public record BookInfo(
      String isbn,
      String title,
      String author,
      String publisher,
      LocalDate publishedDate,
      String description,
      String coverImageUrl,
      boolean saveable) {}
}
