package com.bookshelves.domain.book.exception.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookSuccessCode implements BaseSuccessCode {
  MEMBER_BOOK_STATISTICS_FOUND(HttpStatus.OK, "BOOK200_8", "독서 통계 조회에 성공했습니다."),
  MEMBER_BOOK_CALENDAR_FOUND(HttpStatus.OK, "BOOK200_7", "독서 캘린더 조회에 성공했습니다."),
  MEMBER_BOOK_LIST_FOUND(HttpStatus.OK, "BOOK200_6", "내 서재 목록 조회에 성공했습니다."),
  BOOK_DETAIL_FOUND(HttpStatus.OK, "BOOK200_4", "책 상세 조회에 성공했습니다."),
  CATEGORY_LIST_FOUND(HttpStatus.OK, "BOOK200_1", "카테고리 목록 조회에 성공했습니다."),
  EXTERNAL_BOOK_SEARCHED(HttpStatus.OK, "BOOK200_2", "도서 검색에 성공했습니다."),
  RECENT_BOOK_SEARCHES_FOUND(HttpStatus.OK, "BOOK200_3", "최근 검색어 조회에 성공했습니다."),
  MEMBER_BOOK_CREATED(HttpStatus.CREATED, "BOOK201_1", "독서 기록 저장에 성공했습니다."),
  MEMBER_BOOK_UPDATED(HttpStatus.OK, "BOOK200_5", "독서 기록 수정에 성공했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
