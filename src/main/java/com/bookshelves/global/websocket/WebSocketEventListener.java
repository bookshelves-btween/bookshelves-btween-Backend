package com.bookshelves.global.websocket;

import com.bookshelves.domain.chat.service.ChatPresenceService;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.global.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private static final String CHATROOM_SUBSCRIBE_PREFIX = "/sub/chatrooms/";

  private final ChatPresenceService chatPresenceService;
  private final MeetingCommandService meetingCommandService;

  // 입장자가 JOINED 프레임을 받을 수 있도록 구독 등록 후 presence에 반영한다.
  @EventListener
  public void handleSessionSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    Long chatroomId = parseChatroomId(accessor.getDestination());
    Long memberId = extractMemberId(accessor);

    if (chatroomId == null || memberId == null) {
      return;
    }

    chatPresenceService.join(
        chatroomId, memberId, accessor.getSessionId(), accessor.getSubscriptionId());

    // 출석 저장 실패가 인메모리 presence를 되돌리지 않게 격리한다.
    try {
      meetingCommandService.markAttended(chatroomId, memberId);
    } catch (Exception e) {
      log.warn("출석 처리 실패: chatroomId={}, memberId={}", chatroomId, memberId, e);
    }
  }

  @EventListener
  public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    chatPresenceService.unsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    log.info("WebSocket 해제: sessionId={}", event.getSessionId());
    chatPresenceService.disconnect(event.getSessionId());
  }

  private Long parseChatroomId(String destination) {
    if (destination == null || !destination.startsWith(CHATROOM_SUBSCRIBE_PREFIX)) {
      return null;
    }
    try {
      return Long.parseLong(destination.substring(CHATROOM_SUBSCRIBE_PREFIX.length()));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Long extractMemberId(StompHeaderAccessor accessor) {
    if (accessor.getUser() instanceof Authentication authentication
        && authentication.getPrincipal() instanceof MemberPrincipal principal) {
      return principal.memberId();
    }
    return null;
  }
}
