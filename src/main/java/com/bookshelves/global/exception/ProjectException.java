package com.bookshelves.global.exception;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import java.util.Map;
import lombok.Getter;

@Getter
public class ProjectException extends RuntimeException {

  private final BaseErrorCode errorCode;
  private final Map<String, String> detail;

  public ProjectException(BaseErrorCode errorCode) {
    this(errorCode, Map.of());
  }

  public ProjectException(BaseErrorCode errorCode, Map<String, String> detail) {
    this.errorCode = errorCode;
    this.detail = detail;
  }
}
