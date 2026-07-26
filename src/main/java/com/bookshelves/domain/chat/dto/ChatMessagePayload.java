package com.bookshelves.domain.chat.dto;

import java.time.OffsetDateTime;

public record ChatMessagePayload(
    Long messageId,
    Long senderMemberId,
    String senderNickname,
    String content,
    OffsetDateTime createdAt) {}
