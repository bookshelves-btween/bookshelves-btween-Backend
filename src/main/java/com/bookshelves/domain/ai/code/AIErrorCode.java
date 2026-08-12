package com.bookshelves.domain.ai.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AIErrorCode implements BaseErrorCode {
  VOTE_FORBIDDEN(HttpStatus.FORBIDDEN, "AI403_1", "모임 참여자만 투표할 수 있습니다."),
  ALREADY_VOTED(HttpStatus.CONFLICT, "AI409_1", "이미 이번 질문에 투표했습니다."),
  QUESTION_LIMIT_REACHED(HttpStatus.CONFLICT, "AI409_2", "질문을 더 생성할 수 없습니다."),
  MEETING_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "AI409_3", "진행 중인 모임이 아닙니다.");
  // 삭제된 AI409_4는 재사용하지 않는다.

  private final HttpStatus status;
  private final String code;
  private final String message;
}
