package com.bookshelves.domain.notification.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class NotificationException extends ProjectException {

  public NotificationException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
