package com.bookshelves.domain.ai.dto;

// 새 질문은 triggered 값과 별개로 WebSocket QUESTION 프레임에서 전달한다.
public record QuestionVoteResponse(int currentVotes, int requiredVotes, boolean triggered) {}
