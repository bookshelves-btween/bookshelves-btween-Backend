package com.bookshelves.domain.chat.dto;

// 채팅방 구독에서 사용하는 공통 STOMP 프레임.
public record ChatFrame<T>(String type, Long chatroomId, T data) {

  public static final String TYPE_MESSAGE = "MESSAGE";
  public static final String TYPE_PARTICIPANT = "PARTICIPANT";
  public static final String TYPE_QUESTION = "QUESTION";
  public static final String TYPE_VOTE_COUNT = "VOTE_COUNT";
  public static final String TYPE_SYSTEM = "SYSTEM";

  // 뒤에 chatroomId를 붙여 구독 목적지를 구성한다.
  public static final String CHATROOM_SUB_DESTINATION = "/sub/chatrooms/";

  public static <T> ChatFrame<T> of(String type, Long chatroomId, T data) {
    return new ChatFrame<>(type, chatroomId, data);
  }
}
