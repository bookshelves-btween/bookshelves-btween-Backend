package com.bookshelves.domain.meeting.exception.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingErrorCode implements BaseErrorCode {
  MEETING_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "MEETING400_1", "모임 요청 값이 올바르지 않습니다."),
  MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING404_1", "해당 모임을 찾을 수 없습니다."),
  MEETING_RECRUITMENT_CLOSED(HttpStatus.CONFLICT, "MEETING409_1", "모집이 마감된 모임입니다."),
  DUPLICATE_MEETING(HttpStatus.CONFLICT, "MEETING409_2", "이미 참여한 모임입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
