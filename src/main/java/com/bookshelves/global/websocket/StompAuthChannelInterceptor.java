package com.bookshelves.global.websocket;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.service.ChatQueryService;
import com.bookshelves.global.exception.ProjectException;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.MemberPrincipal;
import com.bookshelves.global.security.TokenType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CHATROOM_SUBSCRIBE_PREFIX = "/sub/chatrooms/";

  private final JwtTokenProvider jwtTokenProvider;
  private final ChatQueryService chatQueryService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT == accessor.getCommand()) {
      accessor.setUser(authenticate(accessor));
    }

    if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
      validateSubscription(accessor);
    }

    return message;
  }

  private Authentication authenticate(StompHeaderAccessor accessor) {
    String token = resolveToken(accessor);

    if (token == null || !jwtTokenProvider.isValidToken(token, TokenType.ACCESS)) {
      throw new ProjectException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

    Long memberId = jwtTokenProvider.getMemberId(token);
    return new UsernamePasswordAuthenticationToken(new MemberPrincipal(memberId), null, List.of());
  }

  private String resolveToken(StompHeaderAccessor accessor) {
    String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }

    return authorization.substring(BEARER_PREFIX.length());
  }

  private void validateSubscription(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();

    if (destination == null || !destination.startsWith(CHATROOM_SUBSCRIBE_PREFIX)) {
      return;
    }

    if (!(accessor.getUser() instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
      throw new ProjectException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

    Long chatroomId = parseChatroomId(destination);

    if (!chatQueryService.isParticipant(chatroomId, principal.memberId())) {
      throw new ProjectException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }
  }

  private Long parseChatroomId(String destination) {
    try {
      return Long.parseLong(destination.substring(CHATROOM_SUBSCRIBE_PREFIX.length()));
    } catch (NumberFormatException e) {
      throw new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND);
    }
  }
}
