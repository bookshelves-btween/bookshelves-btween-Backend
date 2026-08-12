package com.bookshelves.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// POST /api/v1/meetings/{meetingId}/question-votes 응답.
// triggered=true여도 새 질문은 이 응답이 아니라 QUESTION 프레임(SUB)으로 전파된다.
@Schema(description = "다음 질문 공개 투표 결과")
public record QuestionVoteResponse(
    @Schema(description = "현재 라운드에 반영된 투표 수", example = "3") int currentVotes,
    @Schema(description = "다음 질문 공개에 필요한 투표 수", example = "3") int requiredVotes,
    @Schema(description = "이번 투표로 다음 질문이 공개되었는지 여부", example = "true") boolean triggered) {}
