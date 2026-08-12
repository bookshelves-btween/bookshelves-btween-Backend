package com.bookshelves.domain.chat.dto;

// 다음 질문 공개와 투표 카운터 초기화를 알리는 QUESTION 프레임 데이터.
public record ChatQuestionPayload(
    Long questionId, Integer questionOrder, String content, int maxQuestions) {}
