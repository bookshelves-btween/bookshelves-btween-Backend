package com.bookshelves.domain.member.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class MemberException extends ProjectException {

  public MemberException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
