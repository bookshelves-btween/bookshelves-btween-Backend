package com.bookshelves.global.websocket;

// 채팅방 외 STOMP 목적지를 구독 검증과 전송 코드에서 공유한다.
public final class StompDestinations {

  // Spring이 발신자 세션의 사용자 목적지로 치환한다.
  public static final String ERROR_SUB_DESTINATION = "/sub/errors";

  private StompDestinations() {}
}
