package com.bookshelves.domain.chat.dto;

// 채팅방 시스템 이벤트를 전달하는 SYSTEM 프레임 데이터.
public record ChatSystemPayload(String event) {

  public static final String EVENT_MEETING_ENDED = "MEETING_ENDED";

  public static ChatSystemPayload meetingEnded() {
    return new ChatSystemPayload(EVENT_MEETING_ENDED);
  }
}
