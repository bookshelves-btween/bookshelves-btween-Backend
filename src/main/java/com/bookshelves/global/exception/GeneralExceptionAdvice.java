package com.bookshelves.global.exception;

import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.apiPayload.code.GeneralErrorCode;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneralExceptionAdvice {

  // 우리가 만든 예외 처리
  @ExceptionHandler(ProjectException.class)
  public ResponseEntity<ApiResponse<Void>> handleProjectException(ProjectException e) {

    BaseErrorCode code = e.getErrorCode();

    return ResponseEntity.status(code.getStatus()).body(ApiResponse.onFailure(code, null));
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

  // 기타 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<String>> handleException(Exception e) {

    BaseErrorCode code = GeneralErrorCode.COMMON_INTERNAL_SERVER_ERROR;

    return ResponseEntity.status(code.getStatus())
        .body(ApiResponse.onFailure(code, e.getMessage()));
  }
}
