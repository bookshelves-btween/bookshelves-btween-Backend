package com.bookshelves.domain.notification.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI404_1", "존재하지 않는 알림입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
