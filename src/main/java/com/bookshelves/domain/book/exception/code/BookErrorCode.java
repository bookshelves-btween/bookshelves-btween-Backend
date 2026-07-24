package com.bookshelves.domain.book.exception.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {
  INVALID_BOOK_SEARCH_REQUEST(
      HttpStatus.BAD_REQUEST, "BOOK400_1", "검색어, page 또는 size 값이 올바르지 않습니다."),
  BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK404_1", "해당 책을 찾을 수 없습니다."),
  CATEGORY_LIST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_1", "카테고리 목록 조회에 실패했습니다."),
  EXTERNAL_BOOK_API_FAILED(HttpStatus.BAD_GATEWAY, "BOOK502_1", "외부 도서 API 호출에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
