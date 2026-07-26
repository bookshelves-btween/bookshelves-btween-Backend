package com.bookshelves.domain.member.exception;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TermsSuccessCode implements BaseSuccessCode {
  TERMS_LIST_SUCCESS(HttpStatus.OK, "TERMS200_1", "약관 목록 조회에 성공하였습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
