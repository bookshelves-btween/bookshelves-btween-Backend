package com.bookshelves.domain.ai.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Gemini generateContent 호출 규약.
//
// 프롬프트를 쓰는 일과 응답을 검증하는 일은 용도별 클라이언트가 맡는다. 여기는 요청 형식, 응답 껍질
// 벗기기, 세대별 설정만 다룬다. 질문 생성과 요약이 이 코드를 각자 복제하고 있었고, 그 사이에 로그
// 정책 같은 차이가 조용히 벌어졌다.
//
// 응답 본문은 어떤 경우에도 로그로 남기지 않는다. 요약 응답에는 참여자가 꺼낸 개인 경험이 그대로
// 담기는데, 호출부마다 판단하게 두면 한 곳만 실수해도 새어나간다. 전송 계층이 애초에 못 하게 막는다.
// 무엇이 왜 검증에서 떨어졌는지는 각 클라이언트의 validate가 항목 단위로 남긴다.
@Slf4j
public class GeminiClient {

  static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

  // 모델명은 반드시 경로 변수로 넘긴다. gemini-2.0-flash:generateContent를 uri()에 문자열로 그대로 주면
  // 콜론 앞이 URI 스킴으로 파싱돼 baseUrl이 통째로 무시된다(unknown protocol). 템플릿을 /로 시작시키고
  // 확장 후에 콜론이 들어가게 해야 상대 경로로 결합된다.
  static final String GENERATE_CONTENT_PATH = "/models/{model}:generateContent";
  static final String DEFAULT_MODEL = "gemini-3.6-flash";
  private static final String API_KEY_HEADER = "x-goog-api-key";

  // 2.x 이하는 thinking이 없어 낮은 temperature로 변형 폭을 좁힌다.
  //
  // 3.x부터는 반대다. 추론이 기본 temperature(1.0)에 맞춰 조정돼 있어 낮추면 논리가 오히려 흐트러지고
  // 사고 루프에 빠질 수 있다고 구글이 명시한다. 그래서 3.x에서는 temperature를 아예 보내지 않고
  // thinkingLevel로 사고량을 올린다.
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

  // responseMimeType을 JSON으로 지정해도 모델이 ```json 펜스를 덧붙이는 경우가 있어 방어적으로 제거한다
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

  // JSON 강제는 세대 공통, 변형 폭을 좁히는 수단만 세대별로 다르다.
  private GenerationConfig generationConfig() {
    return supportsThinkingLevel()
        ? new GenerationConfig("application/json", null, new ThinkingConfig(THINKING_LEVEL))
        : new GenerationConfig("application/json", LEGACY_TEMPERATURE, null);
  }

  // 3.x 이후를 thinking 계열로 본다. 아는 구세대만 제외해야 새 모델이 조용히 구설정으로 떨어지지 않는다.
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

  // 세대별로 안 쓰는 필드는 아예 보내지 않는다. null이 그대로 직렬화되면 API가 400으로 거절한다.
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record GenerationConfig(
      String responseMimeType, Double temperature, ThinkingConfig thinkingConfig) {}

  private record ThinkingConfig(String thinkingLevel) {}

  private record GeminiResponse(List<Candidate> candidates) {}

  private record Candidate(Content content) {}

  private record Content(List<Part> parts) {}

  private record Part(String text) {}
}
