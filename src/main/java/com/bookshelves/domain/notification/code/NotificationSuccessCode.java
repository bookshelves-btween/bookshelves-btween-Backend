package com.bookshelves.domain.notification.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationSuccessCode implements BaseSuccessCode {
  FCM_TOKEN_REGISTERED(HttpStatus.OK, "NOTI200_1", "FCM 토큰이 등록되었습니다."),
  NOTIFICATION_LIST_FOUND(HttpStatus.OK, "NOTI200_2", "알림 목록 조회에 성공했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
