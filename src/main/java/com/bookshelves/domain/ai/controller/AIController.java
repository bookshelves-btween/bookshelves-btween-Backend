package com.bookshelves.domain.ai.controller;

import com.bookshelves.domain.ai.code.AISuccessCode;
import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.domain.ai.service.AICommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AIController implements AIControllerDocs {

  private final AICommandService aiCommandService;
  private final AuthenticationFacade authenticationFacade;

  @Override
  public ResponseEntity<ApiResponse<QuestionVoteResponse>> voteForNewQuestion(
      @PathVariable Long meetingId) {
    QuestionVoteResponse response =
        aiCommandService.voteForNewQuestion(meetingId, authenticationFacade.getCurrentMemberId());

    return ResponseEntity.status(AISuccessCode.QUESTION_VOTE_SUCCESS.getStatus())
        .body(ApiResponse.onSuccess(AISuccessCode.QUESTION_VOTE_SUCCESS, response));
  }
}
