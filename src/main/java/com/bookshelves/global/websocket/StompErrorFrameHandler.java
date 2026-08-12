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

// CONNECT·SUBSCRIBE 처리 예외를 HTTP와 같은 ApiResponse 형식의 ERROR 프레임으로 변환한다.
// @MessageMapping 처리 예외는 StompExceptionAdvice가 담당한다.
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

  // 래핑된 원인에서 프로젝트 예외를 찾고, 나머지는 내부 정보를 숨긴 공통 오류로 처리한다.
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

  // 직렬화가 실패해도 STOMP message 헤더에는 오류 사유가 남는다.
  private byte[] serialize(BaseErrorCode errorCode) {
    try {
      return objectMapper.writeValueAsBytes(ApiResponse.onFailure(errorCode, Map.of()));
    } catch (Exception e) {
      log.error("STOMP ERROR 프레임 본문 직렬화에 실패했습니다: code={}", errorCode.getCode(), e);
      return new byte[0];
    }
  }
}
