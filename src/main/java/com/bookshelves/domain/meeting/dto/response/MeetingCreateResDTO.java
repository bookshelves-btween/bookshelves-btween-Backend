package com.bookshelves.domain.meeting.dto.response;

import com.bookshelves.domain.meeting.entity.Meeting;

public record MeetingCreateResDTO(Long id) {

  public static MeetingCreateResDTO from(Meeting meeting) {
    return new MeetingCreateResDTO(meeting.getId());
  }
}
