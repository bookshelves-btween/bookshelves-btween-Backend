package com.bookshelves.domain.chat.dto;

// TODO: 인증 기반머지 후 senderId 제거하고 STOMP 세션 Principal에서 발신자 식별
public record ChatMessageRequest(Long senderId, String message) {}
