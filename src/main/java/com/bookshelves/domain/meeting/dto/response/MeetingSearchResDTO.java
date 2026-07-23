package com.bookshelves.domain.meeting.dto.response;

import com.bookshelves.domain.meeting.enums.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 검색 결과")
public record MeetingSearchResDTO(
    List<MeetingInfo> meetings, Integer page, Integer size, boolean hasNext) {

  @Schema(description = "검색된 모임 정보")
  public record MeetingInfo(
      Long id,
      Long chatroomId,
      MeetingStatus status,
      LocalDateTime startDate,
      Integer currentParticipants,
      Integer maxParticipants,
      Integer duration,
      BookInfo book) {}

  @Schema(description = "모임 도서 정보")
  public record BookInfo(Long id, String title, String coverImageUrl) {}
}
