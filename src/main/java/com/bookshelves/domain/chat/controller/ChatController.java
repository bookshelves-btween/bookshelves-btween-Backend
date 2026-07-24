package com.bookshelves.domain.chat.controller;

import com.bookshelves.domain.chat.code.ChatSuccessCode;
import com.bookshelves.domain.chat.dto.ChatRoomEnterResponse;
import com.bookshelves.domain.chat.service.ChatQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatControllerDocs {

  private final ChatQueryService chatQueryService;
  private final AuthenticationFacade authenticationFacade;

  @Override
  public ResponseEntity<ApiResponse<ChatRoomEnterResponse>> enterChatRoom(
      @PathVariable Long chatroomId) {
    ChatRoomEnterResponse response =
        chatQueryService.enterChatRoom(chatroomId, authenticationFacade.getCurrentMemberId());

    return ResponseEntity.status(ChatSuccessCode.CHATROOM_ENTER_SUCCESS.getStatus())
        .body(ApiResponse.onSuccess(ChatSuccessCode.CHATROOM_ENTER_SUCCESS, response));
  }
}
