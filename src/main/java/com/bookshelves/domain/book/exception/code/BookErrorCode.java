package com.bookshelves.domain.book.exception.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseErrorCode {
  MEMBER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK404_2", "내 서재 독서 기록을 찾을 수 없습니다."),
  INVALID_MEMBER_BOOK_STATISTICS_REQUEST(
      HttpStatus.BAD_REQUEST, "BOOK400_6", "조회 연도 또는 월이 올바르지 않습니다."),
  MEMBER_BOOK_STATISTICS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_5", "독서 통계 조회에 실패했습니다."),
  INVALID_MEMBER_BOOK_CALENDAR_REQUEST(
      HttpStatus.BAD_REQUEST, "BOOK400_5", "조회 연도 또는 월이 올바르지 않습니다."),
  MEMBER_BOOK_CALENDAR_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_4", "독서 캘린더 조회에 실패했습니다."),
  INVALID_MEMBER_BOOK_LIST_REQUEST(
      HttpStatus.BAD_REQUEST, "BOOK400_4", "독서 상태, page 또는 size 값이 올바르지 않습니다."),
  MEMBER_BOOK_LIST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_3", "내 서재 목록 조회에 실패했습니다."),
  INVALID_BOOK_ISBN(HttpStatus.BAD_REQUEST, "BOOK400_2", "ISBN 형식이 올바르지 않습니다."),
  INVALID_BOOK_SEARCH_REQUEST(
      HttpStatus.BAD_REQUEST, "BOOK400_1", "검색어, page 또는 size 값이 올바르지 않습니다."),
  INVALID_RECENT_BOOK_SEARCH_DELETE_REQUEST(
      HttpStatus.BAD_REQUEST, "BOOK400_7", "삭제할 최근 검색어가 올바르지 않습니다."),
  MEMBER_BOOK_RATING_CANNOT_BE_CLEARED(
      HttpStatus.BAD_REQUEST, "BOOK400_3", "등록한 평점은 미평가로 변경할 수 없습니다."),
  BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK404_1", "해당 책을 찾을 수 없습니다."),
  CATEGORY_LIST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_1", "카테고리 목록 조회에 실패했습니다."),
  RECENT_BOOK_SEARCHES_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_2", "최근 검색어를 불러오지 못했습니다."),
  RECENT_BOOK_SEARCH_DELETE_FAILED(
      HttpStatus.INTERNAL_SERVER_ERROR, "BOOK500_6", "최근 검색어를 삭제하지 못했습니다."),
  EXTERNAL_BOOK_API_FAILED(HttpStatus.BAD_GATEWAY, "BOOK502_1", "외부 도서 API 호출에 실패했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
