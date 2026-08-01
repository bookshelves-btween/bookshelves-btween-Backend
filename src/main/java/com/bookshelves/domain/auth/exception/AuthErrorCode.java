package com.bookshelves.domain.auth.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  AUTH_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400_1", "지원하지 않는 소셜 로그인 제공자입니다."),
  AUTH_INVALID_PROVIDER_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_1", "유효하지 않은 소셜 토큰입니다."),
  AUTH_INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_2", "유효하지 않은 Access Token입니다."),
  AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_3", "유효하지 않은 Refresh Token입니다."),
  AUTH_INVALID_RESTORE_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_4", "유효하지 않은 계정 복구 토큰입니다."),
  AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH403_1", "접근 권한이 없습니다."),
  AUTH_INVALID_FAKE_SIGN_UP_SECRET(HttpStatus.FORBIDDEN, "AUTH403_3", "테스트 회원가입 비밀값이 올바르지 않습니다."),
  AUTH_UNREISSUABLE_MEMBER_STATUS(HttpStatus.FORBIDDEN, "AUTH403_2", "토큰을 재발급할 수 없는 회원 상태입니다."),
  AUTH_UNRESTORABLE_MEMBER_STATUS(HttpStatus.CONFLICT, "AUTH409_1", "복구할 수 있는 상태의 계정이 아닙니다."),
  AUTH_RESTORE_PERIOD_EXPIRED(HttpStatus.GONE, "AUTH410_1", "계정 복구 가능 기간이 만료되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
