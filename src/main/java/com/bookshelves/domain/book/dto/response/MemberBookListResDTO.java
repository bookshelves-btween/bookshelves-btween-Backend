package com.bookshelves.domain.book.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MemberBookListResDTO(
    List<MemberBookInfo> memberBooks, int page, int size, boolean hasNext) {

  public record MemberBookInfo(MemberBookRecord memberBook, BookInfo book) {}

  public record MemberBookRecord(
      Long id,
      Integer progress,
      String status,
      BigDecimal rating,
      String memo,
      LocalDateTime updatedAt) {}

  public record BookInfo(
      Long id,
      String isbn,
      String title,
      String author,
      String publisher,
      String coverImageUrl,
      String kdcCode,
      String kdcName) {}
}
