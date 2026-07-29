package com.bookshelves.domain.chat.dto;

// SYSTEM 프레임 data — 시스템 이벤트. 현재 event: MEETING_ENDED (모임 종료의 신호)
public record ChatSystemPayload(String event) {

  public static final String EVENT_MEETING_ENDED = "MEETING_ENDED";

  public static ChatSystemPayload meetingEnded() {
    return new ChatSystemPayload(EVENT_MEETING_ENDED);
  }
}
