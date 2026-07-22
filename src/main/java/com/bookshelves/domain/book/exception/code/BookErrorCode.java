package com.bookshelves.domain.book.exception.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {
  BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK404_1", "해당 책을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
