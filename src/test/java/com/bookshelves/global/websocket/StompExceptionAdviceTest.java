package com.bookshelves.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.UserDestinationResult;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

// @MessageMapping 처리 중 예외가 실제로 발신자에게 도달하는지 확인한다.
//
// 이 경로는 배선이 전부다 — @ControllerAdvice 스캔, @MessageExceptionHandler 매칭, @SendToUser
// 반환값 처리, 사용자 목적지 치환. 어느 하나가 빠지면 예외는 조용히 사라지고 클라이언트는 실패를
// 성공과 구분하지 못한다. 그 조용한 실패는 단위 테스트로 드러나지 않아 핸들러를 띄워서 본다.
class StompExceptionAdviceTest {

  private static final String SESSION_ID = "session-1";
  private static final Principal USER =
      new UsernamePasswordAuthenticationToken("member-1", null, List.of());

  private final ExecutorSubscribableChannel clientInbound = new ExecutorSubscribableChannel();
  private final ExecutorSubscribableChannel clientOutbound = new ExecutorSubscribableChannel();
  private final ExecutorSubscribableChannel brokerChannel = new ExecutorSubscribableChannel();
  private final List<Message<?>> toBroker = new ArrayList<>();

  @BeforeEach
  void setUp() {
    brokerChannel.subscribe(toBroker::add);

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(StompExceptionAdvice.class, ThrowingController.class);
    context.refresh();

    SimpMessagingTemplate brokerTemplate = new SimpMessagingTemplate(brokerChannel);
    brokerTemplate.setMessageConverter(new JacksonJsonMessageConverter());

    WebSocketAnnotationMethodMessageHandler handler =
        new WebSocketAnnotationMethodMessageHandler(clientInbound, clientOutbound, brokerTemplate);
    handler.setApplicationContext(context);
    handler.setDestinationPrefixes(List.of("/pub"));
    handler.afterPropertiesSet();
    handler.start();
  }

  @Test
  void domainExceptionBecomesFailureEnvelopeOnTheSenderDestination() {
    clientInbound.send(send("/pub/boom"));

    assertThat(toBroker).hasSize(1);
    // 채팅방 토픽이 아니라 사용자 목적지로 나가야 한 명의 실패가 참여자 전원에게 퍼지지 않는다
    assertThat(SimpMessageHeaderAccessor.getDestination(toBroker.get(0).getHeaders()))
        .endsWith(StompDestinations.ERROR_SUB_DESTINATION)
        .doesNotContain("/sub/chatrooms");

    assertThat(new String((byte[]) toBroker.get(0).getPayload(), StandardCharsets.UTF_8))
        .contains("\"isSuccess\":false")
        .contains(ChatErrorCode.CHATROOM_FORBIDDEN.getCode());
  }

  // 사용자 목적지는 세션 단위로 치환된다. 클라이언트가 구독하는 이름과 서버가 보내는 이름이 같은
  // 실제 목적지로 떨어져야 프레임이 전달된다 — 둘이 어긋나면 오류는 아무 데도 도착하지 않는다.
  @Test
  void senderSubscriptionAndDeliveryResolveToTheSameDestination() {
    clientInbound.send(send("/pub/boom"));

    DefaultUserDestinationResolver resolver =
        new DefaultUserDestinationResolver(connectedRegistry());

    UserDestinationResult delivery = resolver.resolveDestination(toBroker.get(0));
    UserDestinationResult subscription = resolver.resolveDestination(subscribeToErrors());

    assertThat(delivery.getTargetDestinations())
        .isNotEmpty()
        .containsExactlyElementsOf(subscription.getTargetDestinations());
  }

  // 같은 회원이 다른 기기로 함께 보고 있어도 실패는 그 요청을 보낸 세션의 문제다(broadcast = false)
  @Test
  void deliveryIsScopedToTheOriginatingSession() {
    clientInbound.send(send("/pub/boom"));

    UserDestinationResult delivery =
        new DefaultUserDestinationResolver(connectedRegistry()).resolveDestination(toBroker.get(0));

    assertThat(delivery.getSessionIds()).containsExactly(SESSION_ID);
  }

  // 깨진 페이로드는 @Valid까지 가지도 못하고 역직렬화에서 터진다. 클라이언트가 고칠 수 있는
  // 입력 오류이므로 서버 장애(500)와 구분되어야 한다.
  @Test
  void malformedPayloadIsReportedAsBadRequest() {
    clientInbound.send(send("/pub/malformed"));

    assertThat(new String((byte[]) toBroker.get(0).getPayload(), StandardCharsets.UTF_8))
        .contains(GeneralErrorCode.COMMON_BAD_REQUEST.getCode())
        .doesNotContain(GeneralErrorCode.COMMON_INTERNAL_SERVER_ERROR.getCode());
  }

  @Controller
  static class ThrowingController {

    @MessageMapping("/boom")
    public void boom() {
      throw new ChatException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }

    @MessageMapping("/malformed")
    public void malformed() {
      throw new MessageConversionException("역직렬화 실패");
    }
  }

  // 사용자 목적지 해석은 레지스트리에 등록된 세션을 본다. 운영에서는 CONNECT 이벤트가 채우는 자리다.
  private DefaultSimpUserRegistry connectedRegistry() {
    DefaultSimpUserRegistry registry = new DefaultSimpUserRegistry();
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
    accessor.setSessionId(SESSION_ID);
    accessor.setUser(USER);
    accessor.setLeaveMutable(true);
    registry.onApplicationEvent(
        new SessionConnectedEvent(
            this, MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), USER));
    return registry;
  }

  private Message<byte[]> send(String destination) {
    SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    accessor.setDestination(destination);
    accessor.setSessionId(SESSION_ID);
    accessor.setSessionAttributes(new HashMap<>());
    accessor.setUser(USER);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private Message<byte[]> subscribeToErrors() {
    SimpMessageHeaderAccessor accessor =
        SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
    accessor.setDestination(StompExceptionAdvice.ERROR_DESTINATION);
    accessor.setSessionId(SESSION_ID);
    accessor.setSubscriptionId("sub-1");
    accessor.setUser(USER);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }
}
