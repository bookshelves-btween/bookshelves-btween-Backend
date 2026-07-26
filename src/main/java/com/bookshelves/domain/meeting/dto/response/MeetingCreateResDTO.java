package com.bookshelves.domain.meeting.dto.response;

import com.bookshelves.domain.meeting.entity.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 생성 결과")
public record MeetingCreateResDTO(Long id) {

  public static MeetingCreateResDTO from(Meeting meeting) {
    return new MeetingCreateResDTO(meeting.getId());
  }
}
