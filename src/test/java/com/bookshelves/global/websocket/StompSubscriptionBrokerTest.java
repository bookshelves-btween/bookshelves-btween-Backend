package com.bookshelves.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import com.bookshelves.domain.chat.service.ChatSubscriptionValidator;
import com.bookshelves.global.config.JwtProperties;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.MemberPrincipal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

// 인터셉터 단위 테스트가 아니라 실제 SimpleBrokerMessageHandler를 띄워 구독 매칭까지 확인한다.
// 브로커가 구독 목적지를 Ant 패턴으로 매칭한다는 전제 자체를 테스트로 고정해 둔다.
class StompSubscriptionBrokerTest {

  private static final String SESSION_ID = "session-1";
  private static final Long MEMBER_ID = 1L;

  private final ChatSubscriptionValidator chatSubscriptionValidator =
      mock(ChatSubscriptionValidator.class);
  private final StompAuthChannelInterceptor interceptor =
      new StompAuthChannelInterceptor(
          new JwtTokenProvider(
              new JwtProperties("bookshelves-test-jwt-secret-key-value", 3600, 1209600, 600)),
          chatSubscriptionValidator);

  private final ExecutorSubscribableChannel clientInbound = new ExecutorSubscribableChannel();
  private final ExecutorSubscribableChannel clientOutbound = new ExecutorSubscribableChannel();
  private final ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
  private final List<String> deliveredDestinations = new ArrayList<>();

  @BeforeEach
  void setUp() {
    new SimpleBrokerMessageHandler(clientInbound, clientOutbound, brokerChannel, List.of("/sub"))
        .start();
    clientOutbound.subscribe(
        message -> {
          // CONNECT_ACK 등 목적지 없는 프레임은 제외하고 실제 브로드캐스트만 모은다
          String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
          if (destination != null) {
            deliveredDestinations.add(destination);
          }
        });
    // 브로커는 CONNECT로 등록된 세션에만 프레임을 내보낸다 — 인증은 테스트별로 인터셉터를 붙여 확인한다
    clientInbound.send(connect());
  }

  @Test
  void exactSubscriptionReceivesItsOwnChatroomFrames() {
    clientInbound.addInterceptor(interceptor);

    clientInbound.send(subscribe("/sub/chatrooms/1"));
    brokerChannel.send(publish("/sub/chatrooms/1"));

    assertThat(deliveredDestinations).containsExactly("/sub/chatrooms/1");
  }

  @Test
  void wildcardSubscriptionIsRejectedBeforeReachingBroker() {
    clientInbound.addInterceptor(interceptor);

    Throwable thrown = catchThrowable(() -> clientInbound.send(subscribe("/sub/**")));
    brokerChannel.send(publish("/sub/chatrooms/1"));
    brokerChannel.send(publish("/sub/chatrooms/2"));

    assertThat(thrown).isNotNull();
    assertThat(deliveredDestinations).isEmpty();
  }

  // 인터셉터를 걷어내면 `/sub/**` 구독 하나가 모든 채팅방 프레임에 매칭된다.
  // 인터셉터가 유일한 방어선이라는 사실을 명시적으로 남겨 두는 대조군이다.
  @Test
  void brokerItselfMatchesWildcardSubscriptionAcrossChatrooms() {
    clientInbound.send(subscribe("/sub/**"));
    brokerChannel.send(publish("/sub/chatrooms/1"));
    brokerChannel.send(publish("/sub/chatrooms/2"));

    assertThat(deliveredDestinations).containsExactly("/sub/chatrooms/1", "/sub/chatrooms/2");
  }

  private Message<byte[]> connect() {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
    accessor.setSessionId(SESSION_ID);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> subscribe(String destination) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    accessor.setSessionId(SESSION_ID);
    accessor.setSubscriptionId("sub-1");
    accessor.setUser(
        new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of()));
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> publish(String destination) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setDestination(destination);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
