package com.bookshelves.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
    Long messageId,
    Long chatroomId,
    Long senderId,
    String senderNickname,
    String message,
    LocalDateTime sentAt) {}
