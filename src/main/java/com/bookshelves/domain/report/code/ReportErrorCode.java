package com.bookshelves.domain.report.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {
  CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_1", "존재하지 않는 채팅방입니다."),
  NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "REPORT403_1", "모임 참여자만 신고할 수 있습니다."),
  ALREADY_REPORTED(HttpStatus.CONFLICT, "REPORT409_1", "이미 신고한 채팅방입니다."),
  MEETING_NOT_STARTED(HttpStatus.CONFLICT, "REPORT409_2", "모임이 시작된 뒤에 신고할 수 있습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
