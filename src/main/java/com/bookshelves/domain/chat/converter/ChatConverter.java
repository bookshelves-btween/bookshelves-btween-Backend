package com.bookshelves.domain.chat.converter;

import com.bookshelves.domain.chat.dto.ChatMessageResponse;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConverter {

  public static ChatMessage toChatMessage(ChatRoom chatRoom, Member sender, String message) {
    return ChatMessage.builder().chatRoom(chatRoom).senderMember(sender).message(message).build();
  }

  public static ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
    return new ChatMessageResponse(
        chatMessage.getId(),
        chatMessage.getChatRoom().getId(),
        chatMessage.getSenderMember().getId(),
        chatMessage.getSenderMember().getNickname(),
        chatMessage.getMessage(),
        chatMessage.getCreatedAt());
  }
}
