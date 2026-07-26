package com.bookshelves.domain.meeting.converter;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO.BookInfo;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO.SummaryInfo;
import com.bookshelves.domain.meeting.dto.response.MeetingSearchResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingSearchResDTO.MeetingInfo;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

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
    boolean canProvideSummary =
        meeting.getStatus() == MeetingStatus.COMPLETED && !meetingSummaries.isEmpty();

    return new MeetingDetailResDTO(
        meeting.getId(),
        chatroomId,
        meeting.getStatus(),
        meeting.getStartDate(),
        meeting.getDuration(),
        meeting.getCurParticipants(),
        meeting.getMaxParticipants(),
        toBookInfo(meeting.getBook()),
        canProvideSummary
            ? meetingSummaries.stream().map(MeetingConverter::toSummaryInfo).toList()
            : null);
  }

  public static MeetingSearchResDTO toMeetingSearchResDTO(
      Page<Meeting> meetingPage, Map<Long, Long> chatroomIds) {
    List<MeetingInfo> meetings =
        meetingPage.getContent().stream()
            .map(meeting -> toMeetingInfo(meeting, chatroomIds.get(meeting.getId())))
            .toList();

    return new MeetingSearchResDTO(
        meetings, meetingPage.getNumber() + 1, meetingPage.getSize(), meetingPage.hasNext());
  }

  private static MeetingInfo toMeetingInfo(Meeting meeting, Long chatroomId) {
    Book book = meeting.getBook();
    return new MeetingInfo(
        meeting.getId(),
        chatroomId,
        meeting.getStatus(),
        meeting.getStartDate(),
        meeting.getCurParticipants(),
        meeting.getMaxParticipants(),
        meeting.getDuration(),
        new MeetingSearchResDTO.BookInfo(book.getId(), book.getTitle(), book.getCoverImageUrl()));
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
