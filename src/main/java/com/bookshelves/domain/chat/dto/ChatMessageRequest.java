package com.bookshelves.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(@NotBlank String message) {}
