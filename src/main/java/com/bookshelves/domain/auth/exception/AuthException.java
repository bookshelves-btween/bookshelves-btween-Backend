package com.bookshelves.domain.auth.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class AuthException extends ProjectException {

  public AuthException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
