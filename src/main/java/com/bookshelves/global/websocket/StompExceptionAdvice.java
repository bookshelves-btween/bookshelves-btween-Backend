package com.bookshelves.global.websocket;

import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import com.bookshelves.global.exception.ProjectException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

// @MessageMapping 처리 예외를 HTTP와 같은 ApiResponse 형식으로 발신자에게만 전달한다.
@Slf4j
@ControllerAdvice
public class StompExceptionAdvice {

  // Spring이 세션별로 분리하는 클라이언트 구독 목적지.
  public static final String ERROR_DESTINATION = "/user" + StompDestinations.ERROR_SUB_DESTINATION;

  @MessageExceptionHandler(ProjectException.class)
  @SendToUser(destinations = StompDestinations.ERROR_SUB_DESTINATION, broadcast = false)
  public ApiResponse<Map<String, Object>> handleProjectException(ProjectException e) {
    return ApiResponse.onFailure(e.getErrorCode(), Map.of());
  }

  // 검증 및 역직렬화 실패는 모두 잘못된 요청으로 처리한다.
  @MessageExceptionHandler({
    MethodArgumentNotValidException.class,
    MessageConversionException.class
  })
  @SendToUser(destinations = StompDestinations.ERROR_SUB_DESTINATION, broadcast = false)
  public ApiResponse<Map<String, Object>> handleBadPayloadException(Exception e) {
    return ApiResponse.onFailure(GeneralErrorCode.COMMON_BAD_REQUEST, Map.of());
  }

  // 예상하지 못한 예외의 상세 정보는 서버 로그에만 남긴다.
  @MessageExceptionHandler(Exception.class)
  @SendToUser(destinations = StompDestinations.ERROR_SUB_DESTINATION, broadcast = false)
  public ApiResponse<Map<String, Object>> handleException(Exception e) {
    log.error("STOMP 메시지 처리 중 처리되지 않은 예외가 발생했습니다.", e);

    return ApiResponse.onFailure(GeneralErrorCode.COMMON_INTERNAL_SERVER_ERROR, Map.of());
  }
}
