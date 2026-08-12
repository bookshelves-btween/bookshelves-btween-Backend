package com.bookshelves.global.websocket;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.domain.chat.service.ChatSubscriptionValidator;
import com.bookshelves.global.security.AccessTokenGuard;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.MemberPrincipal;
import com.bookshelves.global.security.TokenType;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.springframework.util.AntPathMatcher;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  // 브로커와 사용자 목적지 구독은 모두 권한 검증 대상이다.
  private static final String BROKER_DESTINATION_PREFIX = "/sub";
  private static final String USER_DESTINATION_PREFIX = "/user";
  // 구독을 허용하는 유일한 목적지 형태. 채팅방 ID는 양의 정수만 받는다.
  private static final Pattern CHATROOM_DESTINATION =
      Pattern.compile("^" + Pattern.quote(ChatFrame.CHATROOM_SUB_DESTINATION) + "([1-9][0-9]*)$");
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final JwtTokenProvider jwtTokenProvider;
  private final AccessTokenGuard accessTokenGuard;
  private final ChatSubscriptionValidator chatSubscriptionValidator;

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
      throw new AuthException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

    Long memberId = jwtTokenProvider.getMemberId(token);

    // HTTP와 같은 접근 조건을 적용하며 구체적인 거부 사유는 노출하지 않는다.
    if (!accessTokenGuard.grantsAccess(memberId, jwtTokenProvider.getIssuedAt(token))) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

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

    // 브로커가 처리하지 않는 목적지는 검증 대상에서 제외한다.
    if (destination == null
        || !(destination.startsWith(BROKER_DESTINATION_PREFIX)
            || destination.startsWith(USER_DESTINATION_PREFIX))) {
      return;
    }

    if (!(accessor.getUser() instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
    }

    // 세션별 오류 목적지만 정확히 일치하도록 허용한다.
    if (StompExceptionAdvice.ERROR_DESTINATION.equals(destination)) {
      return;
    }
    if (destination.startsWith(USER_DESTINATION_PREFIX)) {
      throw new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND);
    }

    Long chatroomId = parseChatroomId(destination);

    chatSubscriptionValidator.validate(chatroomId, principal.memberId());
  }

  // Ant 패턴을 차단해 하나의 구독으로 여러 채팅방 프레임을 받지 못하게 한다.
  private Long parseChatroomId(String destination) {
    Matcher matcher = CHATROOM_DESTINATION.matcher(destination);

    if (!matcher.matches() || PATH_MATCHER.isPattern(destination)) {
      throw new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND);
    }

    try {
      return Long.parseLong(matcher.group(1));
    } catch (NumberFormatException e) {
      throw new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND);
    }
  }
}
