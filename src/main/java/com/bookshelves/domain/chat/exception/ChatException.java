package com.bookshelves.domain.chat.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class ChatException extends ProjectException {

  public ChatException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
