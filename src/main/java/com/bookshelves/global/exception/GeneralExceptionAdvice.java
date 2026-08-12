package com.bookshelves.global.exception;

import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

  // 우리가 만든 예외 처리
  @ExceptionHandler(ProjectException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleProjectException(
      ProjectException e) {
    return failureResponse(e.getErrorCode());
  }

  // @Valid 요청 검증 실패
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
      MethodArgumentNotValidException e) {

    BaseErrorCode code = GeneralErrorCode.COMMON_BAD_REQUEST;

    Map<String, String> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    fieldError ->
                        fieldError.getDefaultMessage() == null
                            ? ""
                            : fieldError.getDefaultMessage(),
                    (existing, replacement) -> existing));

    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, fieldErrors));
  }

  // 쿼리 파라미터 제약 조건 및 타입 검증 실패
  @ExceptionHandler({
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleRequestParameterException(
      Exception e) {
    return failureResponse(GeneralErrorCode.COMMON_BAD_REQUEST);
  }

  // 요청 본문 파싱 실패 (JSON 문법 오류, 존재하지 않는 enum 값 등)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException e) {
    return failureResponse(GeneralErrorCode.COMMON_BAD_REQUEST);
  }

  // Content-Type 미지원
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException e) {
    return failureResponse(GeneralErrorCode.COMMON_UNSUPPORTED_MEDIA_TYPE);
  }

  // 매핑된 컨트롤러도, 정적 리소스도 없는 경로 요청. 프로파일에 따라 등록되지 않는
  // 컨트롤러로 오는 요청도 여기로 떨어진다.
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleNoResourceFoundException(
      NoResourceFoundException e) {
    return failureResponse(GeneralErrorCode.COMMON_NOT_FOUND);
  }

  // 존재하는 경로를 지원하지 않는 HTTP 메서드로 호출한 요청. 어떤 메서드가 허용되는지
  // Allow 헤더로 함께 알려준다(RFC 9110).
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>>
      handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
    return failureResponse(GeneralErrorCode.COMMON_METHOD_NOT_ALLOWED, e.getHeaders());
  }

  // 기타 예외 — 원인은 서버 로그로만 남기고, 클라이언트에는 내부 구현 정보를 노출하지 않는다.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleException(Exception e) {
    log.error("처리되지 않은 예외가 발생했습니다.", e);

    return failureResponse(GeneralErrorCode.COMMON_INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ApiResponse<Map<String, Object>>> failureResponse(BaseErrorCode code) {
    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, Map.of()));
  }

  private ResponseEntity<ApiResponse<Map<String, Object>>> failureResponse(
      BaseErrorCode code, HttpHeaders headers) {
    return ResponseEntity.status(code.getStatus())
        .headers(headers)
        .body(ApiResponse.onFailure(code, Map.of()));
  }
}
