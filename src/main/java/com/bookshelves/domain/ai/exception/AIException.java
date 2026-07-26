package com.bookshelves.domain.ai.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class AIException extends ProjectException {

  public AIException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
