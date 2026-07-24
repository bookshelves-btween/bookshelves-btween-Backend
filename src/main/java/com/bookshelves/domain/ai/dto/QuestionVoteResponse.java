package com.bookshelves.domain.ai.dto;

// POST /api/v1/meetings/{meetingId}/question-votes 응답.
// triggered=true여도 새 질문은 이 응답이 아니라 QUESTION 프레임(SUB)으로 전파된다.
public record QuestionVoteResponse(int currentVotes, int requiredVotes, boolean triggered) {}
