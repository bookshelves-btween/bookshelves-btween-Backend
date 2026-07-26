package com.bookshelves.domain.report.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportSuccessCode implements BaseSuccessCode {
  REPORT_CREATED(HttpStatus.CREATED, "REPORT201_1", "신고가 접수되었습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
