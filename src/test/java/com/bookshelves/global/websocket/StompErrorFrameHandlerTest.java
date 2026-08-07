package com.bookshelves.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.exception.ChatException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

class StompErrorFrameHandlerTest {

  private final StompErrorFrameHandler handler = new StompErrorFrameHandler(new ObjectMapper());

  @Test
  void domainExceptionBecomesFailureEnvelope() {
    Message<byte[]> error =
        handler.handleClientMessageProcessingError(
            subscribe(), new ChatException(ChatErrorCode.CHATROOM_FORBIDDEN));

    assertThat(body(error))
        .contains("\"isSuccess\":false")
        .contains("\"code\":\"" + ChatErrorCode.CHATROOM_FORBIDDEN.getCode() + "\"")
        .contains(ChatErrorCode.CHATROOM_FORBIDDEN.getMessage());
    assertThat(StompHeaderAccessor.wrap(error).getMessage())
        .isEqualTo(ChatErrorCode.CHATROOM_FORBIDDEN.getMessage());
  }

  // 채널이 인터셉터 예외를 MessageDeliveryException으로 감싸므로 원인 사슬을 따라가야 한다
  @Test
  void wrappedDomainExceptionKeepsItsErrorCode() {
    Message<byte[]> error =
        handler.handleClientMessageProcessingError(
            subscribe(),
            new MessageDeliveryException(
                subscribe(), "전달 실패", new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND)));

    assertThat(body(error))
        .contains("\"code\":\"" + ChatErrorCode.CHATROOM_NOT_FOUND.getCode() + "\"");
  }

  // 우리 예외가 아니면 내부 구현 정보를 노출하지 않고 공통 500으로 내린다
  @Test
  void unknownExceptionDoesNotLeakItsMessage() {
    Message<byte[]> error =
        handler.handleClientMessageProcessingError(
            subscribe(), new IllegalStateException("커넥션 풀 pool-1 고갈"));

    assertThat(body(error)).contains("COMMON500_1").doesNotContain("pool-1");
    assertThat(StompHeaderAccessor.wrap(error).getMessage()).doesNotContain("pool-1");
  }

  @Test
  void receiptIdIsCarriedBackToClient() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setReceipt("receipt-7");
    accessor.setLeaveMutable(true);
    Message<byte[]> clientMessage =
        MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<byte[]> error =
        handler.handleClientMessageProcessingError(
            clientMessage, new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND));

    assertThat(StompHeaderAccessor.wrap(error).getReceiptId()).isEqualTo("receipt-7");
  }

  private Message<byte[]> subscribe() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/sub/chatrooms/1");
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private String body(Message<byte[]> message) {
    return new String(message.getPayload(), StandardCharsets.UTF_8);
  }
}
