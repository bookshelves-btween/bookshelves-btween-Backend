package com.bookshelves.domain.chat.dto;

// SUB /sub/chatrooms/{id} 수신 프레임 공통 envelope — type: MESSAGE·QUESTION·VOTE_COUNT·PARTICIPANT·SYSTEM
public record ChatFrame<T>(String type, Long chatroomId, T data) {

  public static final String TYPE_MESSAGE = "MESSAGE";
  public static final String TYPE_PARTICIPANT = "PARTICIPANT";

  public static <T> ChatFrame<T> of(String type, Long chatroomId, T data) {
    return new ChatFrame<>(type, chatroomId, data);
  }
}
