package com.bookshelves.domain.home.exception.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum HomeSuccessCode implements BaseSuccessCode {
  HOME_FOUND(HttpStatus.OK, "HOME200_1", "홈 화면 조회에 성공했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
