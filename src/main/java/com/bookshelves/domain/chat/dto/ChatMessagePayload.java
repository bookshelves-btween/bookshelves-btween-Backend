package com.bookshelves.domain.chat.dto;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import java.time.OffsetDateTime;

public record ChatMessagePayload(
    Long messageId,
    Long senderMemberId,
    String senderNickname,
    String senderNicknameAnimal,
    ProfileBackgroundColor senderProfileBackgroundColor,
    String content,
    OffsetDateTime createdAt) {}
