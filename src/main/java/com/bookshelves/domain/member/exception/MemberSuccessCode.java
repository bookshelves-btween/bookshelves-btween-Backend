package com.bookshelves.domain.member.exception;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {
  MY_INFO_SUCCESS(HttpStatus.OK, "MEMBER200", "내 정보 조회에 성공하였습니다."),
  UPDATE_SUCCESS(HttpStatus.OK, "MEMBER200", "회원 정보 수정에 성공하였습니다."),
  WITHDRAW_SUCCESS(HttpStatus.OK, "MEMBER200", "회원 탈퇴가 요청되었습니다."),
  ONBOARDING_SUCCESS(HttpStatus.OK, "MEMBER200", "온보딩이 완료되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
