package com.bookshelves.domain.ai.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Gemini generateContent의 요청·응답 형식과 모델별 생성 설정을 관리한다.
// 응답에는 개인적인 대화가 포함될 수 있으므로 본문을 로그로 남기지 않는다.
@Slf4j
public class GeminiClient {

  static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

  // 모델명의 콜론이 URI 스킴으로 해석되지 않도록 경로 변수로 치환한다.
  static final String GENERATE_CONTENT_PATH = "/models/{model}:generateContent";
  static final String DEFAULT_MODEL = "gemini-3.6-flash";
  private static final String API_KEY_HEADER = "x-goog-api-key";

  // 2.x 이하는 temperature를, 3.x 이상은 thinkingLevel을 사용한다.
  private static final double LEGACY_TEMPERATURE = 0.2;
  private static final String THINKING_LEVEL = "high";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  GeminiClient(RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.model = model;
  }

  /**
   * 프롬프트를 보내고 JSON 배열 응답을 파싱해 돌려준다.
   *
   * <p>HTTP 예외는 잡지 않고 그대로 올린다. 재시도할 만한 실패인지는 호출부가 상태 코드로 판단한다.
   *
   * @return 파싱된 항목. 내용 검증은 호출부의 몫이다.
   */
  public <T> List<T> generate(String prompt, TypeReference<List<T>> type) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
    }

    GeminiResponse response =
        restClient
            .post()
            .uri(GENERATE_CONTENT_PATH, model)
            .header(API_KEY_HEADER, apiKey)
            .body(GeminiRequest.of(prompt, generationConfig()))
            .retrieve()
            .body(GeminiResponse.class);

    String json = extractText(response);
    if (json == null || json.isBlank()) {
      throw new IllegalStateException("Gemini 응답에서 본문을 추출하지 못했습니다.");
    }
    log.debug("Gemini 응답 수신: model={}, length={}", model, json.length());

    try {
      return objectMapper.readValue(stripCodeFence(json), type);
    } catch (Exception e) {
      throw new IllegalStateException("Gemini 응답 JSON 파싱에 실패했습니다.", e);
    }
  }

  // JSON 응답에 코드 펜스가 붙는 경우를 허용한다.
  private String stripCodeFence(String json) {
    String trimmed = json.strip();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }
    int start = trimmed.indexOf('\n');
    int end = trimmed.lastIndexOf("```");
    if (start < 0 || end <= start) {
      return trimmed;
    }
    return trimmed.substring(start + 1, end).strip();
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

  private GenerationConfig generationConfig() {
    return supportsThinkingLevel()
        ? new GenerationConfig("application/json", null, new ThinkingConfig(THINKING_LEVEL))
        : new GenerationConfig("application/json", LEGACY_TEMPERATURE, null);
  }

  // 새 모델이 레거시 설정으로 분류되지 않도록 알려진 구세대만 제외한다.
  private boolean supportsThinkingLevel() {
    return !model.startsWith("gemini-1.") && !model.startsWith("gemini-2.");
  }

  private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    private static GeminiRequest of(String prompt, GenerationConfig generationConfig) {
      List<Content> contents = new ArrayList<>();
      contents.add(new Content(List.of(new Part(prompt))));
      return new GeminiRequest(contents, generationConfig);
    }
  }

  // 모델 세대에 사용하지 않는 필드는 요청에서 제외한다.
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record GenerationConfig(
      String responseMimeType, Double temperature, ThinkingConfig thinkingConfig) {}

  private record ThinkingConfig(String thinkingLevel) {}

  private record GeminiResponse(List<Candidate> candidates) {}

  private record Candidate(Content content) {}

  private record Content(List<Part> parts) {}

  private record Part(String text) {}
}
