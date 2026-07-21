package com.bookshelves.domain.auth.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400", "지원하지 않는 소셜 로그인 제공자입니다."),
  INVALID_PROVIDER_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "유효하지 않은 소셜 토큰입니다."),
  INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "유효하지 않은 Access Token입니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "유효하지 않은 Refresh Token입니다."),
  INVALID_RESTORE_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "유효하지 않은 계정 복구 토큰입니다."),
  EXPIRED_RESTORE_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401", "계정 복구 토큰이 만료되었습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH403", "접근 권한이 없습니다."),
  UNREISSUABLE_MEMBER_STATUS(HttpStatus.FORBIDDEN, "AUTH403", "토큰을 재발급할 수 없는 회원 상태입니다."),
  UNRESTORABLE_MEMBER_STATUS(HttpStatus.CONFLICT, "AUTH409", "복구할 수 있는 상태의 계정이 아닙니다."),
  RESTORE_PERIOD_EXPIRED(HttpStatus.GONE, "AUTH410", "계정 복구 가능 기간이 만료되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
