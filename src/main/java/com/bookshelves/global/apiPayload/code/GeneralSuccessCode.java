package com.bookshelves.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {
  COMMON_OK(HttpStatus.OK, "COMMON200", "성공입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
