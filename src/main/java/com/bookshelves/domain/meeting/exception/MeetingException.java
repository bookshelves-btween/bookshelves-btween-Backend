package com.bookshelves.domain.meeting.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class MeetingException extends ProjectException {

  public MeetingException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
