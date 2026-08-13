package com.bookshelves.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.global.apiPayload.ApiResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GeneralExceptionAdviceTest {

  private final GeneralExceptionAdvice generalExceptionAdvice = new GeneralExceptionAdvice();

  @Test
  void handleProjectExceptionReturnsEmptyResult() {
    ProjectException exception = new ProjectException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);

    ResponseEntity<ApiResponse<Map<String, String>>> response =
        generalExceptionAdvice.handleProjectException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody().getCode()).isEqualTo("AUTH401_2");
    assertThat(response.getBody().getResult()).isEmpty();
  }

  @Test
  void handleProjectExceptionReturnsDetailWhenPresent() {
    ProjectException exception =
        new ProjectException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN, Map.of("field", "잘못된 값입니다."));

    ResponseEntity<ApiResponse<Map<String, String>>> response =
        generalExceptionAdvice.handleProjectException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody().getResult()).containsEntry("field", "잘못된 값입니다.");
  }

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
    assertThat(response.getBody().getCode()).isEqualTo("COMMON400_1");
    assertThat(response.getBody().getResult()).containsEntry("provider", "널이어서는 안됩니다");
  }

  @Test
  void handleHttpMessageNotReadableExceptionReturnsBadRequest() {
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        generalExceptionAdvice.handleHttpMessageNotReadableException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON400_1");
    assertThat(response.getBody().getResult()).isEmpty();
  }

  @Test
  void handleHttpMediaTypeNotSupportedExceptionReturnsUnsupportedMediaType() {
    HttpMediaTypeNotSupportedException exception = mock(HttpMediaTypeNotSupportedException.class);

    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        generalExceptionAdvice.handleHttpMediaTypeNotSupportedException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON415_1");
    assertThat(response.getBody().getResult()).isEmpty();
  }

  @Test
  void handleNoResourceFoundExceptionReturnsNotFound() {
    NoResourceFoundException exception =
        new NoResourceFoundException(
            HttpMethod.POST, "api/v1/does-not-exist", "/api/v1/does-not-exist");

    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        generalExceptionAdvice.handleNoResourceFoundException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON404_1");
    assertThat(response.getBody().getResult()).isEmpty();
  }

  @Test
  void handleHttpRequestMethodNotSupportedExceptionReturnsMethodNotAllowedWithAllowHeader() {
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("GET", List.of("POST"));

    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        generalExceptionAdvice.handleHttpRequestMethodNotSupportedException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON405_1");
    assertThat(response.getBody().getResult()).isEmpty();
    assertThat(response.getHeaders().getAllow()).containsExactly(HttpMethod.POST);
  }

  @Test
  void handleExceptionReturnsInternalServerErrorWithoutLeakingMessage() {
    Exception exception = new IllegalStateException("com.bookshelves.internal.SecretDetail");

    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        generalExceptionAdvice.handleException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().getCode()).isEqualTo("COMMON500_1");
    assertThat(response.getBody().getResult()).isEmpty();
    assertThat(response.getBody().getMessage()).doesNotContain("SecretDetail");
  }
}
