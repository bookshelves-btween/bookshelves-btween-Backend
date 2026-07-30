package com.bookshelves.domain.book.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MemberBookCalendarResDTO(int year, int month, List<CalendarDay> days) {

  public record CalendarDay(LocalDate date, List<CalendarBook> books) {}

  public record CalendarBook(
      Long historyId,
      Long memberBookId,
      Integer progress,
      Long bookId,
      String title,
      String coverImageUrl) {}
}
