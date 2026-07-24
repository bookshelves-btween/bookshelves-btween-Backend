package com.bookshelves.domain.chat.dto;

// QUESTION 프레임 data — 정족수 도달로 새 AI 질문이 생성되면 상단 질문 교체 + 투표 카운터 리셋
public record ChatQuestionPayload(
    Long questionId, Integer questionOrder, String content, int maxQuestions) {}
