package com.bookshelves.domain.chat.controller;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.chat.dto.ChatMessageRequest;
import com.bookshelves.domain.chat.dto.ChatMessageResponse;
import com.bookshelves.domain.chat.service.ChatCommandService;
import com.bookshelves.global.exception.ProjectException;
import com.bookshelves.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

  private final ChatCommandService chatCommandService;

  @MessageMapping("/chatrooms/{chatroomId}")
  @SendTo("/sub/chatrooms/{chatroomId}")
  public ChatMessageResponse sendMessage(
      @DestinationVariable Long chatroomId,
      @Payload @Valid ChatMessageRequest request,
      Principal principal) {
    return chatCommandService.saveMessage(chatroomId, extractMemberId(principal), request);
  }

  private Long extractMemberId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
      return memberPrincipal.memberId();
    }
    throw new ProjectException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
  }
}
