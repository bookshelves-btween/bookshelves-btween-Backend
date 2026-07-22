package com.bookshelves.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.global.apiPayload.ApiResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GeneralExceptionAdviceTest {

  private final GeneralExceptionAdvice generalExceptionAdvice = new GeneralExceptionAdvice();

  @Test
  void handleValidationExceptionReturnsBadRequestWithFieldErrors() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors())
        .thenReturn(List.of(new FieldError("socialLoginRequest", "provider", "널이어서는 안됩니다")));

    ResponseEntity<ApiResponse<Map<String, String>>> response =
        generalExceptionAdvice.handleValidationException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON400");
    assertThat(response.getBody().getResult()).containsEntry("provider", "널이어서는 안됩니다");
  }

  @Test
  void handleHttpMessageNotReadableExceptionReturnsBadRequest() {
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

    ResponseEntity<ApiResponse<Void>> response =
        generalExceptionAdvice.handleHttpMessageNotReadableException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON400");
  }

  @Test
  void handleHttpMediaTypeNotSupportedExceptionReturnsUnsupportedMediaType() {
    HttpMediaTypeNotSupportedException exception = mock(HttpMediaTypeNotSupportedException.class);

    ResponseEntity<ApiResponse<Void>> response =
        generalExceptionAdvice.handleHttpMediaTypeNotSupportedException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON415");
  }
}
