package com.bookshelves.domain.meeting.dto.response;

import com.bookshelves.domain.meeting.entity.MeetingParticipant;

public record MeetingParticipationResDTO(Long meetingParticipantId) {

  public static MeetingParticipationResDTO from(MeetingParticipant meetingParticipant) {
    return new MeetingParticipationResDTO(meetingParticipant.getId());
  }
}
