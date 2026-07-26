package com.bookshelves.domain.book.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookDetailResDTO(BookInfo book, MemberBookInfo memberBook) {

  public record BookInfo(
      Long id,
      String isbn,
      String title,
      String author,
      String publisher,
      LocalDate publishedDate,
      String description,
      String coverImageUrl,
      String kdcCode,
      String kdcName) {}

  public record MemberBookInfo(Long id, Integer progress, BigDecimal rating, String memo) {}
}
