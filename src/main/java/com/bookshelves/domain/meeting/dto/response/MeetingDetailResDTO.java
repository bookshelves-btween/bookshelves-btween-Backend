package com.bookshelves.domain.meeting.dto.response;

import com.bookshelves.domain.meeting.enums.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 상세 조회 결과")
public record MeetingDetailResDTO(
    Long id,
    Long chatroomId,
    MeetingStatus status,
    LocalDateTime startDate,
    Integer duration,
    Integer currentParticipants,
    Integer maxParticipants,
    BookInfo book,
    List<SummaryInfo> meetingSummary) {

  @Schema(description = "모임 도서 정보")
  public record BookInfo(
      Long id,
      String title,
      String description,
      String author,
      String publisher,
      String coverImageUrl,
      String kdcName) {}

  @Schema(description = "모임 요약 주제")
  public record SummaryInfo(String title, String summary) {}
}
