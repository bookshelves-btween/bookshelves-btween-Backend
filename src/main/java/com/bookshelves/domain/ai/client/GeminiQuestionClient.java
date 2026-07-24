package com.bookshelves.domain.ai.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Gemini generateContent 호출로 다음 토론 질문 한 문장을 생성한다.
// 실패(키 미설정·네트워크·빈 응답)는 예외로 전파하고, 폴백 질문 사용 여부는 호출부가 결정한다.
@Component
public class GeminiQuestionClient {

  private static final String MODEL = "gemini-2.0-flash";
  private static final String API_KEY_HEADER = "x-goog-api-key";

  private final RestClient restClient;
  private final String apiKey;

  public GeminiQuestionClient(
      RestClient.Builder restClientBuilder, @Value("${external.gemini.api-key}") String apiKey) {
    this.restClient =
        restClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/")
            .build();
    this.apiKey = apiKey;
  }

  public String generateQuestion(
      String bookTitle, String bookAuthor, List<String> previousQuestions) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
    }

    GeminiResponse response =
        restClient
            .post()
            .uri(MODEL + ":generateContent")
            .header(API_KEY_HEADER, apiKey)
            .body(GeminiRequest.of(buildPrompt(bookTitle, bookAuthor, previousQuestions)))
            .retrieve()
            .body(GeminiResponse.class);

    String question = extractText(response);
    if (question == null || question.isBlank()) {
      throw new IllegalStateException("Gemini 응답에서 질문을 추출하지 못했습니다.");
    }
    return question.trim();
  }

  private String buildPrompt(String bookTitle, String bookAuthor, List<String> previousQuestions) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("당신은 독서 모임 진행자입니다. 아래 책에 대한 토론 질문을 하나 만들어 주세요.\n")
        .append("책 제목: ")
        .append(bookTitle);
    if (bookAuthor != null && !bookAuthor.isBlank()) {
      prompt.append("\n저자: ").append(bookAuthor);
    }
    if (!previousQuestions.isEmpty()) {
      prompt.append("\n\n이미 나온 질문(겹치지 않게 새로운 관점으로):");
      previousQuestions.forEach(question -> prompt.append("\n- ").append(question));
    }
    prompt.append("\n\n조건: 참여자들이 자기 생각을 말하고 싶어지는 열린 질문, 한국어 한 문장, 질문 문장만 출력.");
    return prompt.toString();
  }

  private String extractText(GeminiResponse response) {
    if (response == null
        || response.candidates() == null
        || response.candidates().isEmpty()
        || response.candidates().get(0).content() == null
        || response.candidates().get(0).content().parts() == null
        || response.candidates().get(0).content().parts().isEmpty()) {
      return null;
    }
    return response.candidates().get(0).content().parts().get(0).text();
  }

  private record GeminiRequest(List<Content> contents) {
    private static GeminiRequest of(String prompt) {
      return new GeminiRequest(List.of(new Content(List.of(new Part(prompt)))));
    }
  }

  private record GeminiResponse(List<Candidate> candidates) {}

  private record Candidate(Content content) {}

  private record Content(List<Part> parts) {}

  private record Part(String text) {}
}
