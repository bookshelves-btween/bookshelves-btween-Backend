package com.bookshelves.domain.meeting.converter;

import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class MeetingConverter {

  private MeetingConverter() {}

  public static Meeting toEntity(Book book, MeetingCreateReqDTO request) {
    return Meeting.builder()
        .book(book)
        .startDate(LocalDateTime.of(request.startDate(), LocalTime.parse(request.startTime())))
        .duration(request.duration())
        .maxParticipants(request.maxParticipants())
        .build();
  }
}
