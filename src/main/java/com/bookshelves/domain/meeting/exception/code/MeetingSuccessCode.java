package com.bookshelves.domain.meeting.exception.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingSuccessCode implements BaseSuccessCode {
  MEETING_CREATED(HttpStatus.CREATED, "MEETING201", "모임이 생성되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
