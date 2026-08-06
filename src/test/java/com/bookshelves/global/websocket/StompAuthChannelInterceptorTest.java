package com.bookshelves.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.domain.chat.service.ChatSubscriptionValidator;
import com.bookshelves.global.config.JwtProperties;
import com.bookshelves.global.security.AccessTokenGuard;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.MemberPrincipal;
import com.bookshelves.global.security.TokenType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class StompAuthChannelInterceptorTest {

  private static final Long MEMBER_ID = 1L;

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(
          new JwtProperties("bookshelves-test-jwt-secret-key-value", 3600, 1209600, 600));
  private final AccessTokenGuard accessTokenGuard = mock(AccessTokenGuard.class);
  private final ChatSubscriptionValidator chatSubscriptionValidator =
      mock(ChatSubscriptionValidator.class);
  private final StompAuthChannelInterceptor interceptor =
      new StompAuthChannelInterceptor(
          jwtTokenProvider, accessTokenGuard, chatSubscriptionValidator);
  private final MessageChannel channel = mock(MessageChannel.class);

  @Test
  void exactChatroomDestinationIsValidated() {
    interceptor.preSend(subscribe("/sub/chatrooms/12", authenticated()), channel);

    verify(chatSubscriptionValidator).validate(12L, MEMBER_ID);
  }

  // 브로커는 구독 목적지를 접두사가 아니라 Ant 패턴으로 등록·매칭하므로,
  // 패턴 목적지 하나로 모든 채팅방 프레임을 받는 우회가 성립해선 안 된다
  @ParameterizedTest
  @ValueSource(
      strings = {
        "/sub/**",
        "/sub/*",
        "/sub/chatrooms/**",
        "/sub/chatrooms/*",
        "/sub/chatrooms/?",
        "/sub/chatrooms/{chatroomId}"
      })
  void patternDestinationIsRejectedWithoutReachingValidator(String destination) {
    assertThatThrownBy(() -> interceptor.preSend(subscribe(destination, authenticated()), channel))
        .isInstanceOf(ChatException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHATROOM_NOT_FOUND);

    verify(chatSubscriptionValidator, never()).validate(anyLong(), anyLong());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/sub",
        "/sub/",
        "/sub/chatrooms",
        "/sub/chatrooms/",
        "/sub/chatrooms/1/extra",
        "/sub/chatrooms/0",
        "/sub/chatrooms/-1",
        "/sub/chatrooms/01",
        "/sub/chatrooms/1a",
        "/sub/chatrooms/99999999999999999999",
        "/subterfuge/chatrooms/1"
      })
  void malformedBrokerDestinationIsRejected(String destination) {
    assertThatThrownBy(() -> interceptor.preSend(subscribe(destination, authenticated()), channel))
        .isInstanceOf(ChatException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHATROOM_NOT_FOUND);

    verify(chatSubscriptionValidator, never()).validate(anyLong(), anyLong());
  }

  // 브로커 목적지가 아니면 구독이 등록되지 않아 어떤 프레임도 전달되지 않는다
  @Test
  void nonBrokerDestinationIsIgnored() {
    assertThatCode(
            () -> interceptor.preSend(subscribe("/pub/chatrooms/1", authenticated()), channel))
        .doesNotThrowAnyException();

    verifyNoInteractions(chatSubscriptionValidator);
  }

  // 처리 실패를 받는 사용자 목적지. 세션별로 치환되므로 채팅방 참여 검증 대상이 아니다.
  @Test
  void errorDestinationIsAllowedWithoutChatroomValidation() {
    assertThatCode(
            () ->
                interceptor.preSend(
                    subscribe(StompExceptionAdvice.ERROR_DESTINATION, authenticated()), channel))
        .doesNotThrowAnyException();

    verifyNoInteractions(chatSubscriptionValidator);
  }

  // 오류 목적지는 정확히 일치할 때만 통과한다 — 사용자 목적지 아래를 열어두면 치환 규칙에 기대는
  // 우회 여지가 생긴다
  @ParameterizedTest
  @ValueSource(
      strings = {"/user/**", "/user/sub/**", "/user/sub/errors/1", "/user/sub/chatrooms/1"})
  void otherUserDestinationIsRejected(String destination) {
    assertThatThrownBy(() -> interceptor.preSend(subscribe(destination, authenticated()), channel))
        .isInstanceOf(ChatException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHATROOM_NOT_FOUND);

    verifyNoInteractions(chatSubscriptionValidator);
  }

  @Test
  void unauthenticatedErrorDestinationSubscribeIsRejected() {
    assertThatThrownBy(
            () ->
                interceptor.preSend(
                    subscribe(StompExceptionAdvice.ERROR_DESTINATION, null), channel))
        .isInstanceOf(AuthException.class);
  }

  @Test
  void unauthenticatedSubscribeIsRejected() {
    assertThatThrownBy(() -> interceptor.preSend(subscribe("/sub/chatrooms/1", null), channel))
        .isInstanceOf(AuthException.class);

    verifyNoInteractions(chatSubscriptionValidator);
  }

  @Test
  void connectSetsPrincipalWhenGuardGrantsAccess() {
    given(accessTokenGuard.grantsAccess(eq(MEMBER_ID), any())).willReturn(true);
    Message<byte[]> message = connect(jwtTokenProvider.generateToken(MEMBER_ID, TokenType.ACCESS));

    interceptor.preSend(message, channel);

    Object principal =
        ((Authentication) StompHeaderAccessor.wrap(message).getUser()).getPrincipal();
    assertThat(principal).isEqualTo(new MemberPrincipal(MEMBER_ID));
  }

  // 탈퇴·정지 회원과 로그아웃된 토큰이 웹소켓으로만 들어오는 비대칭을 막는다.
  // 서명이 유효해도 HTTP 필터와 같은 판정을 통과하지 못하면 CONNECT를 거부한다.
  @Test
  void connectIsRejectedWhenGuardDeniesAccess() {
    given(accessTokenGuard.grantsAccess(eq(MEMBER_ID), any())).willReturn(false);
    String token = jwtTokenProvider.generateToken(MEMBER_ID, TokenType.ACCESS);

    assertThatThrownBy(() -> interceptor.preSend(connect(token), channel))
        .isInstanceOf(AuthException.class);
  }

  @Test
  void connectWithInvalidTokenIsRejectedWithoutConsultingGuard() {
    assertThatThrownBy(() -> interceptor.preSend(connect("not-a-token"), channel))
        .isInstanceOf(AuthException.class);

    verifyNoInteractions(accessTokenGuard);
  }

  private Authentication authenticated() {
    return new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of());
  }

  private Message<byte[]> connect(String token) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setSessionId("session-1");
    accessor.setNativeHeader("Authorization", "Bearer " + token);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> subscribe(String destination, Authentication user) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setSessionId("session-1");
    accessor.setSubscriptionId("sub-1");
    accessor.setUser(user);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
