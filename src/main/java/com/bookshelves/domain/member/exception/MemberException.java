package com.bookshelves.domain.member.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;
import java.util.Map;

public class MemberException extends ProjectException {

  public MemberException(BaseErrorCode errorCode) {
    super(errorCode);
  }

  public MemberException(BaseErrorCode errorCode, Map<String, String> detail) {
    super(errorCode, detail);
  }
}
