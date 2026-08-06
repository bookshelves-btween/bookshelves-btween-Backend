package com.bookshelves.domain.chat.dto;

import com.bookshelves.domain.chat.entity.ChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
    @NotBlank @Size(max = ChatMessage.MAX_MESSAGE_LENGTH) String content) {}
