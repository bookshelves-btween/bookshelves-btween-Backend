package com.bookshelves.domain.chat.controller;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.dto.ChatMessageRequest;
import com.bookshelves.domain.chat.service.ChatCommandService;
import com.bookshelves.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

  private final ChatCommandService chatCommandService;
  private final SimpMessagingTemplate messagingTemplate;

  // DB에서 생성된 ID와 시각을 사용하도록 저장 후 전송한다.
  @MessageMapping("/chatrooms/{chatroomId}")
  public void sendMessage(
      @DestinationVariable Long chatroomId,
      @Payload @Valid ChatMessageRequest request,
      Principal principal) {
    chatCommandService
        .saveMessage(chatroomId, extractMemberId(principal), request.content())
        .ifPresent(
            payload ->
                messagingTemplate.convertAndSend(
                    ChatFrame.CHATROOM_SUB_DESTINATION + chatroomId,
                    ChatFrame.of(ChatFrame.TYPE_MESSAGE, chatroomId, payload)));
  }

  private Long extractMemberId(Principal principal) {
    if (principal instanceof Authentication authentication
        && authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
      return memberPrincipal.memberId();
    }
    throw new AuthException(AuthErrorCode.AUTH_INVALID_ACCESS_TOKEN);
  }
}
