package com.bookshelves.global.websocket;

// STOMP 목적지 중 채팅방에 속하지 않는 것들. 채팅 프레임 목적지는 ChatFrame이 갖는다.
//
// 구독 허용 목록(StompAuthChannelInterceptor)과 전송 지점(StompExceptionAdvice)이 같은 문자열을
// 봐야 한다. 한쪽만 바뀌면 클라이언트가 구독은 되는데 프레임이 안 오거나, 그 반대가 된다.
public final class StompDestinations {

  // 처리 실패를 발신자에게만 돌려주는 사용자 목적지. 서버는 이 값으로 보내고 Spring이 세션별
  // 목적지로 치환한다. 클라이언트는 앞에 /user를 붙여 구독한다.
  public static final String ERROR_SUB_DESTINATION = "/sub/errors";

  private StompDestinations() {}
}
