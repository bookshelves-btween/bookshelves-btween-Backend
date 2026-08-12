package com.bookshelves.domain.ai.event;

// 질문 커서 커밋 후 WebSocket 전송을 요청하는 내부 이벤트.
public record QuestionRevealedEvent(Long chatroomId, Long meetingId, int questionOrder) {}
