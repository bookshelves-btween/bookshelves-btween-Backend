package com.bookshelves.domain.chat.controller;

import com.bookshelves.domain.chat.dto.ChatMessageRequest;
import com.bookshelves.domain.chat.dto.ChatMessageResponse;
import com.bookshelves.domain.chat.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

  private final ChatCommandService chatCommandService;

  @MessageMapping("/chatrooms/{chatroomId}")
  @SendTo("/sub/chatrooms/{chatroomId}")
  public ChatMessageResponse sendMessage(
      @DestinationVariable Long chatroomId, ChatMessageRequest request) {
    return chatCommandService.saveMessage(chatroomId, request);
  }
}
