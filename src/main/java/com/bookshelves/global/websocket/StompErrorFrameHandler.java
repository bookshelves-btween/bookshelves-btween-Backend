package com.bookshelves.global.websocket;

import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import com.bookshelves.global.exception.ProjectException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import tools.jackson.databind.ObjectMapper;

// CONNECT·SUBSCRIBE 처리 중 발생한 예외를 ERROR 프레임에 실어 보낸다.
//
// @RestControllerAdvice는 서블릿 디스패처 안에서만 동작해 STOMP 경로에는 개입하지 않는다.
// 기본 핸들러는 예외 메시지 원문을 그대로 message 헤더에 넣으므로, 클라이언트는 HTTP에서 받던
// ApiResponse envelope 대신 형식이 다른 문자열을 받게 된다. 여기서 같은 envelope로 맞춘다.
//
// 인터셉터가 던진 예외는 clientInboundChannel의 send가 호출 스레드에서 그대로 올려주므로
// 여기까지 도달한다. 반면 @MessageMapping 처리 중 예외는 다른 스레드에서 발생해 이 경로를 타지
// 않는다 — 그쪽은 StompExceptionAdvice가 맡는다.
@Slf4j
@Component
@RequiredArgsConstructor
public class StompErrorFrameHandler extends StompSubProtocolErrorHandler {

  private final ObjectMapper objectMapper;

  @Override
  public Message<byte[]> handleClientMessageProcessingError(
      Message<byte[]> clientMessage, Throwable ex) {
    BaseErrorCode errorCode = resolveErrorCode(ex);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
    accessor.setMessage(errorCode.getMessage());
    accessor.setContentType(MediaType.APPLICATION_JSON);
    accessor.setLeaveMutable(true);
    copyReceipt(clientMessage, accessor);

    return MessageBuilder.createMessage(serialize(errorCode), accessor.getMessageHeaders());
  }

  // 채널이 예외를 MessageDeliveryException으로 감싸므로 원인 사슬을 따라가 우리 예외를 찾는다.
  // 못 찾으면 내부 구현 정보를 노출하지 않고 공통 500으로 내린다 — HTTP 쪽 처리와 같은 기준이다.
  private BaseErrorCode resolveErrorCode(Throwable ex) {
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof ProjectException projectException) {
        return projectException.getErrorCode();
      }
      if (cause.getCause() == cause) {
        break;
      }
    }
    log.error("STOMP 처리 중 예상하지 못한 예외가 발생했습니다.", ex);
    return GeneralErrorCode.COMMON_INTERNAL_SERVER_ERROR;
  }

  private void copyReceipt(Message<byte[]> clientMessage, StompHeaderAccessor accessor) {
    if (clientMessage == null) {
      return;
    }
    StompHeaderAccessor clientAccessor =
        MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
    if (clientAccessor != null && clientAccessor.getReceipt() != null) {
      accessor.setReceiptId(clientAccessor.getReceipt());
    }
  }

  // 직렬화 실패가 원래 오류를 삼키면 안 된다. 본문이 비어도 message 헤더로 사유는 전달된다.
  private byte[] serialize(BaseErrorCode errorCode) {
    try {
      return objectMapper.writeValueAsBytes(ApiResponse.onFailure(errorCode, Map.of()));
    } catch (Exception e) {
      log.error("STOMP ERROR 프레임 본문 직렬화에 실패했습니다: code={}", errorCode.getCode(), e);
      return new byte[0];
    }
  }
}
