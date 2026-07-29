package com.bookshelves.domain.ai.event;

// 정족수 도달로 다음 질문이 공개된 사건. 커밋 후 broadcast를 위해 사용하는 내부 이벤트다.
// 커서 증가가 커밋되기 전에 QUESTION 프레임을 보내면, 롤백 시 존재하지 않는 질문이 전파된다.
public record QuestionRevealedEvent(Long chatroomId, Long meetingId, int questionOrder) {}
