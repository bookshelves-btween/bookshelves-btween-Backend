package com.bookshelves.domain.ai.controller;

import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "AI", description = "AI 질문·요약 API")
public interface AIControllerDocs {

  @Operation(
      summary = "AI 새 질문 생성 투표",
      description =
          "채팅 화면 하단 \"질문 생성하기\" 버튼 투표. 정족수는 접속자 기준 ceil(connected/2). "
              + "투표 반영 시 VOTE_COUNT 프레임이 전원에게 broadcast되고, 정족수 도달(triggered=true) 시 "
              + "AI 질문을 비동기 생성해 QUESTION 프레임으로 전파하며 투표 라운드가 리셋된다. "
              + "에러: 403 비참여자(AI403_1) · 409 중복 투표(AI409_1) · 409 질문 5개 한도(AI409_2) · "
              + "409 진행 중 아님(AI409_3) · 404 모임 없음(MEETING404_1)")
  @PostMapping("/api/v1/meetings/{meetingId}/question-votes")
  ResponseEntity<ApiResponse<QuestionVoteResponse>> voteForNewQuestion(
      @PathVariable Long meetingId);
}
