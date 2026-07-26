package com.bookshelves.domain.report.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import com.bookshelves.global.exception.ProjectException;

public class ReportException extends ProjectException {

  public ReportException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
