package com.bookshelves.domain.book.exception.code;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BookErrorCodeTest {

  @Test
  void errorMessagesMatchApiSpecification() {
    assertErrorCode(
        BookErrorCode.MEMBER_BOOK_RATING_CANNOT_BE_CLEARED,
        "BOOK400_3",
        "등록한 평점은 미평가로 변경할 수 없습니다.");
    assertErrorCode(
        BookErrorCode.INVALID_MEMBER_BOOK_CALENDAR_REQUEST, "BOOK400_5", "조회 연도 또는 월이 올바르지 않습니다.");
    assertErrorCode(
        BookErrorCode.INVALID_MEMBER_BOOK_STATISTICS_REQUEST,
        "BOOK400_6",
        "조회 연도 또는 월이 올바르지 않습니다.");
  }

  private void assertErrorCode(BookErrorCode errorCode, String code, String message) {
    assertThat(errorCode.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(errorCode.getCode()).isEqualTo(code);
    assertThat(errorCode.getMessage()).isEqualTo(message);
  }
}
