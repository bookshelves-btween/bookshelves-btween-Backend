package com.bookshelves.domain.chat.dto;

// SUB /sub/chatrooms/{id} 수신 프레임 공통 envelope — type: MESSAGE·QUESTION·VOTE_COUNT·PARTICIPANT·SYSTEM
public record ChatFrame<T>(String type, Long chatroomId, T data) {

  public static final String TYPE_MESSAGE = "MESSAGE";
  public static final String TYPE_PARTICIPANT = "PARTICIPANT";
  public static final String TYPE_QUESTION = "QUESTION";
  public static final String TYPE_VOTE_COUNT = "VOTE_COUNT";
  public static final String TYPE_SYSTEM = "SYSTEM";

  // 프레임이 발행되는 구독 채널 prefix — 뒤에 chatroomId를 붙여 사용
  public static final String CHATROOM_SUB_DESTINATION = "/sub/chatrooms/";

  public static <T> ChatFrame<T> of(String type, Long chatroomId, T data) {
    return new ChatFrame<>(type, chatroomId, data);
  }
}
