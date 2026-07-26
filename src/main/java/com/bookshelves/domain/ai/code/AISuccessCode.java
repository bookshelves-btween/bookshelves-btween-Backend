package com.bookshelves.domain.ai.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AISuccessCode implements BaseSuccessCode {
  QUESTION_VOTE_SUCCESS(HttpStatus.OK, "AI200_1", "질문 생성 투표가 반영되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
