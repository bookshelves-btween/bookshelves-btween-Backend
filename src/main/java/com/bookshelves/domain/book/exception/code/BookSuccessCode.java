package com.bookshelves.domain.book.exception.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookSuccessCode implements BaseSuccessCode {
  CATEGORY_LIST_FOUND(HttpStatus.OK, "BOOK200_1", "카테고리 목록 조회에 성공했습니다."),
  EXTERNAL_BOOK_SEARCHED(HttpStatus.OK, "BOOK200_2", "도서 검색에 성공했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
