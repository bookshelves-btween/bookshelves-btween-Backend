package com.bookshelves.domain.chat.code;

import com.bookshelves.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {
  CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404_1", "존재하지 않는 채팅방입니다."),
  SENDER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404_2", "존재하지 않는 회원입니다."),
  CHATROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT403_1", "채팅방에 참여하지 않은 회원입니다."),
  CHATROOM_ENDED(HttpStatus.GONE, "CHAT410_1", "이미 종료된 모임입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
