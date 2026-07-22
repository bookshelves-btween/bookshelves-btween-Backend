package com.bookshelves.domain.auth.exception;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {
  AUTH_SOCIAL_LOGIN_SUCCESS(HttpStatus.OK, "AUTH200_1", "소셜 로그인에 성공했습니다."),
  AUTH_LOGOUT_SUCCESS(HttpStatus.OK, "AUTH200_2", "로그아웃에 성공하였습니다."),
  AUTH_REISSUE_SUCCESS(HttpStatus.OK, "AUTH200_3", "토큰 재발급에 성공하였습니다."),
  AUTH_RESTORE_SUCCESS(HttpStatus.OK, "AUTH200_4", "계정 복구에 성공하였습니다."),
  AUTH_WITHDRAWN_ACCOUNT(HttpStatus.ACCEPTED, "AUTH202_1", "탈퇴 대기 중인 계정입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
