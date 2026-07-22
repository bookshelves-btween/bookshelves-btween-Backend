package com.bookshelves.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
public class WebSocketEventListener {

  // TODO: 채팅방별 접속자 추적 — AI 새 질문 생성 투표(접속자 과반수) 계산에 사용

  @EventListener
  public void handleSessionConnected(SessionConnectedEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    log.info("WebSocket 연결: sessionId={}", accessor.getSessionId());
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    log.info("WebSocket 해제: sessionId={}", event.getSessionId());
  }
}
