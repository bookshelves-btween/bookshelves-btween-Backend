package com.bookshelves.domain.meeting.converter;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO.BookInfo;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO.SummaryInfo;
import com.bookshelves.domain.meeting.entity.Meeting;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

  public static MeetingDetailResDTO toMeetingDetailResDTO(
      Meeting meeting, Long chatroomId, List<MeetingSummary> meetingSummaries) {
    boolean summaryCompleted = !meetingSummaries.isEmpty();

    return new MeetingDetailResDTO(
        meeting.getId(),
        chatroomId,
        meeting.getStatus(),
        meeting.getStartDate(),
        meeting.getDuration(),
        meeting.getCurParticipants(),
        meeting.getMaxParticipants(),
        toBookInfo(meeting.getBook()),
        summaryCompleted
            ? meetingSummaries.stream().map(MeetingConverter::toSummaryInfo).toList()
            : null);
  }

  private static BookInfo toBookInfo(Book book) {
    return new BookInfo(
        book.getId(),
        book.getTitle(),
        book.getDescription(),
        book.getAuthor(),
        book.getPublisher(),
        book.getCoverImageUrl(),
        book.getKdcName());
  }

  private static SummaryInfo toSummaryInfo(MeetingSummary meetingSummary) {
    return new SummaryInfo(
        meetingSummary.getAiQuestion().getQuestionOrder(),
        meetingSummary.getAiQuestion().getContent(),
        meetingSummary.getContent());
  }
}
