package com.bookshelves.global.websocket;

import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import com.bookshelves.global.exception.ProjectException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

// @MessageMapping 처리 중 발생한 예외를 발신자에게 돌려준다.
//
// 이 예외들은 clientInboundChannel의 실행 스레드에서 발생해 인터셉터 예외와 달리 ERROR 프레임
// 경로(StompErrorFrameHandler)로 올라오지 않는다. @MessageExceptionHandler가 없으면 프레임워크가
// 로그만 남기고 끝나서, 메시지 전송이 실패해도 클라이언트는 성공과 구분하지 못한다.
//
// 실패는 채팅방 토픽이 아니라 보낸 사람 한 명에게만 간다(broadcast = false). 토픽으로 내보내면
// 한 명의 실패가 참여자 전원에게 퍼지고, 남의 실패 사유까지 보이게 된다.
// 클라이언트는 ERROR_DESTINATION을 구독해 받는다. 본문 형식은 HTTP와 같은 ApiResponse envelope다.
// WebSocketAnnotationMethodMessageHandler가 @ControllerAdvice 빈에서 @MessageExceptionHandler를
// 찾아 전역 등록한다. 컨트롤러마다 핸들러를 복제하지 않아도 된다.
@Slf4j
@ControllerAdvice
public class StompExceptionAdvice {

  // 클라이언트가 구독하는 목적지. 사용자 목적지라 Spring이 세션별로 치환하므로 남의 프레임은 오지 않는다.
  public static final String ERROR_DESTINATION = "/user" + StompDestinations.ERROR_SUB_DESTINATION;

  @MessageExceptionHandler(ProjectException.class)
  @SendToUser(destinations = StompDestinations.ERROR_SUB_DESTINATION, broadcast = false)
  public ApiResponse<Map<String, Object>> handleProjectException(ProjectException e) {
    return ApiResponse.onFailure(e.getErrorCode(), Map.of());
  }

  // @Valid 페이로드 검증 실패 — 메시징 계층은 웹 MVC와 다른 MethodArgumentNotValidException을 던진다
  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser(destinations = StompDestinations.ERROR_SUB_DESTINATION, broadcast = false)
  public ApiResponse<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException e) {
    return ApiResponse.onFailure(GeneralErrorCode.COMMON_BAD_REQUEST, Map.of());
  }

  // 기타 예외 — 원인은 서버 로그로만 남기고, 클라이언트에는 내부 구현 정보를 노출하지 않는다
  @MessageExceptionHandler(Exception.class)
  @SendToUser(destinations = StompDestinations.ERROR_SUB_DESTINATION, broadcast = false)
  public ApiResponse<Map<String, Object>> handleException(Exception e) {
    log.error("STOMP 메시지 처리 중 처리되지 않은 예외가 발생했습니다.", e);

    return ApiResponse.onFailure(GeneralErrorCode.COMMON_INTERNAL_SERVER_ERROR, Map.of());
  }
}
