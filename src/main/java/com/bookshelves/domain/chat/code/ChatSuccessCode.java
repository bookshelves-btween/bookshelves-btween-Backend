package com.bookshelves.domain.chat.code;

import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatSuccessCode implements BaseSuccessCode {
  CHATROOM_ENTER_SUCCESS(HttpStatus.OK, "CHAT200_1", "채팅방 입장에 성공했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
