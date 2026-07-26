package com.bookshelves.domain.member.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404_1", "회원을 찾을 수 없습니다."),
  MEMBER_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "MEMBER400_1", "유효하지 않은 요청입니다."),
  MEMBER_NO_FIELDS_TO_UPDATE(HttpStatus.BAD_REQUEST, "MEMBER400_2", "수정할 회원 정보가 없습니다."),
  MEMBER_ALREADY_ONBOARDED(HttpStatus.CONFLICT, "MEMBER409_1", "이미 온보딩을 완료한 회원입니다."),
  MEMBER_NOT_ACTIVE(HttpStatus.CONFLICT, "MEMBER409_2", "탈퇴할 수 없는 회원 상태입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
