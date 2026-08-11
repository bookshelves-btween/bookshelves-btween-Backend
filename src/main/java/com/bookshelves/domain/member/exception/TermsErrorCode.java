package com.bookshelves.domain.member.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TermsErrorCode implements BaseErrorCode {
  TERMS_REQUIRED_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERMS400_1", "필수 약관에 동의하지 않으셨습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
