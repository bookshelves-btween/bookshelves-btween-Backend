package com.bookshelves.domain.ai.converter;

import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.chat.dto.ChatQuestionPayload;

public class AIConverter {

  private AIConverter() {}

  public static QuestionVoteResponse toQuestionVoteResponse(
      int currentVotes, int requiredVotes, boolean triggered) {
    return new QuestionVoteResponse(currentVotes, requiredVotes, triggered);
  }

  public static ChatQuestionPayload toChatQuestionPayload(AIQuestion question, int maxQuestions) {
    return new ChatQuestionPayload(
        question.getId(), question.getQuestionOrder(), question.getContent(), maxQuestions);
  }
}
