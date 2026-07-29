package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.book.entity.Book;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// 공통 시드 질문을 "그 책에 맞게" 각색한다. 질문을 새로 만들지 않는다.
//
// 각색 근거는 오직 프롬프트로 넣어준 책 정보뿐이다. 모델의 내부 지식에 기대면 마이너한 책·신간·동명이서에서
// 존재하지 않는 등장인물이나 설정을 지어내고, 응답 자체는 정상이라 폴백도 걸리지 않는다(조용한 실패).
// 그래서 판정 기준을 "모델이 아는가"가 아니라 "우리가 준 정보로 구체화되는가"로 옮긴다.
//
// 반환값은 검증을 통과한 항목만 담는다. 빠진 순서는 호출부가 시드 원문으로 채운다.
@Slf4j
@Component
public class GeminiQuestionClient {

  static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

  // 모델명은 반드시 경로 변수로 넘긴다. "gemini-2.0-flash:generateContent"를 uri()에 문자열로 그대로 주면
  // 콜론 앞이 URI 스킴으로 파싱돼 baseUrl이 통째로 무시된다(unknown protocol). 템플릿을 "/"로 시작시키고
  // 확장 후에 콜론이 들어가게 해야 상대 경로로 결합된다.
  static final String GENERATE_CONTENT_PATH = "/models/{model}:generateContent";
  static final String MODEL = "gemini-2.0-flash";
  private static final String API_KEY_HEADER = "x-goog-api-key";

  // 책 소개가 이보다 짧으면 각색 근거가 없다고 보고 호출 자체를 생략한다(비용·지연 절약).
  private static final int MIN_DESCRIPTION_LENGTH = 30;

  // 각색본 길이 상한 배수 — 원문의 의도를 유지한 구체화는 길이가 크게 늘지 않는다.
  private static final int MAX_LENGTH_RATIO = 2;
  private static final int MIN_LENGTH = 10;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  @Autowired
  public GeminiQuestionClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${external.gemini.api-key}") String apiKey) {
    this(buildRestClient(restClientBuilder), objectMapper, apiKey);
  }

  GeminiQuestionClient(RestClient restClient, ObjectMapper objectMapper, String apiKey) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
  }

  private static RestClient buildRestClient(RestClient.Builder restClientBuilder) {
    // LLM 응답 지연이 준비 스레드를 계속 붙잡지 않도록 타임아웃을 건다 — 초과 시 시드 원문으로 진행
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(20));

    return restClientBuilder.baseUrl(BASE_URL).requestFactory(requestFactory).build();
  }

  /**
   * 시드 질문 전체를 LLM 1회 호출로 각색한다.
   *
   * @return question_order → 각색된 질문. 검증을 통과한 항목만 담기며, 각색할 근거가 없으면 빈 Map.
   */
  public Map<Integer, String> adaptSeedQuestions(Book book) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
    }
    String description = book.getDescription();
    if (description == null || description.strip().length() < MIN_DESCRIPTION_LENGTH) {
      // 제목·저자만으로는 "정보에 근거한 각색"이 성립하지 않는다 — 원문 사용
      return Map.of();
    }

    GeminiResponse response =
        restClient
            .post()
            .uri(GENERATE_CONTENT_PATH, MODEL)
            .header(API_KEY_HEADER, apiKey)
            .body(GeminiRequest.of(buildPrompt(book)))
            .retrieve()
            .body(GeminiResponse.class);

    String json = extractText(response);
    if (json == null || json.isBlank()) {
      throw new IllegalStateException("Gemini 응답에서 각색 결과를 추출하지 못했습니다.");
    }
    return validate(parse(json));
  }

  private String buildPrompt(Book book) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("당신은 독서 모임 진행자입니다. 아래 '공통 질문'을 이 책에 맞게 각색해 주세요.\n\n")
        .append("[책 정보]\n")
        .append("제목: ")
        .append(book.getTitle())
        .append('\n');
    appendIfPresent(prompt, "저자", book.getAuthor());
    appendIfPresent(prompt, "출판사", book.getPublisher());
    appendIfPresent(prompt, "분류", book.getKdcName());
    appendIfPresent(prompt, "소개", book.getDescription());

    prompt.append("\n[공통 질문]\n");
    SeedQuestion.ordered()
        .forEach(
            seed ->
                prompt
                    .append(seed.getQuestionOrder())
                    .append(". ")
                    .append(seed.getContent())
                    .append('\n'));

    prompt
        .append("\n[규칙]\n")
        .append("1. 위 [책 정보]에 적힌 내용에만 근거해 각색하세요. ")
        .append("[책 정보]에 없는 등장인물·사건·설정·결말을 지어내면 안 됩니다.\n")
        .append("2. 질문의 의도와 답변 형식은 그대로 유지하세요. ")
        .append("문장을 새로 쓰지 말고, 이 책에 맞게 구체화만 하세요.\n")
        .append("3. 각색할 근거가 부족한 질문은 adapted를 false로 두고 원문을 그대로 반환하세요. ")
        .append("5개를 모두 바꿀 필요는 없습니다. 형식만 묻는 질문은 대개 원문이 낫습니다.\n")
        .append("4. 각 질문은 한국어 한두 문장, 원문보다 크게 길어지지 않게 하세요.\n\n")
        .append("[출력 형식] 다른 설명 없이 아래 JSON 배열만 출력하세요.\n")
        .append("[{\"order\": 1, \"adapted\": true, \"question\": \"...\"}, ...]");
    return prompt.toString();
  }

  private void appendIfPresent(StringBuilder prompt, String label, String value) {
    if (value != null && !value.isBlank()) {
      prompt.append(label).append(": ").append(value.strip()).append('\n');
    }
  }

  private List<AdaptedQuestion> parse(String json) {
    try {
      return objectMapper.readValue(stripCodeFence(json), new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Gemini 각색 응답 JSON 파싱에 실패했습니다.", e);
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

  // LLM 응답을 그대로 믿지 않는다 — 규칙을 어긴 항목은 버리고 호출부가 시드 원문을 쓰게 한다.
  private Map<Integer, String> validate(List<AdaptedQuestion> candidates) {
    Map<Integer, SeedQuestion> seedsByOrder = new HashMap<>();
    SeedQuestion.ordered().forEach(seed -> seedsByOrder.put(seed.getQuestionOrder(), seed));

    Map<Integer, String> accepted = new LinkedHashMap<>();
    for (AdaptedQuestion candidate : candidates) {
      if (candidate == null || candidate.order() == null || !candidate.adapted()) {
        continue;
      }
      SeedQuestion seed = seedsByOrder.get(candidate.order());
      if (seed == null || accepted.containsKey(candidate.order())) {
        continue; // 모르는 순서이거나 중복 — 먼저 온 것만 채택
      }
      String question = normalize(candidate.question());
      if (question == null
          || question.length() < MIN_LENGTH
          || question.length() > seed.getContent().length() * MAX_LENGTH_RATIO) {
        log.warn("각색 질문이 길이 검증에 걸려 원문을 사용한다: order={}", candidate.order());
        continue;
      }
      accepted.put(candidate.order(), question);
    }
    return accepted;
  }

  private String normalize(String question) {
    if (question == null || question.isBlank()) {
      return null;
    }
    return question.strip().replaceAll("\\s+", " ");
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

  // 모델이 형식 밖의 필드를 덧붙여도 파싱 전체가 실패하지 않도록 미지 필드를 무시한다 —
  // 필드 하나 때문에 5개 각색이 전부 원문으로 떨어지는 것을 막는다
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record AdaptedQuestion(Integer order, boolean adapted, String question) {}

  private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    private static GeminiRequest of(String prompt) {
      List<Content> contents = new ArrayList<>();
      contents.add(new Content(List.of(new Part(prompt))));
      // JSON 강제 + 낮은 temperature — 각색은 창작이 아니라 제한된 변형이다
      return new GeminiRequest(contents, new GenerationConfig("application/json", 0.2));
    }
  }

  private record GenerationConfig(String responseMimeType, double temperature) {}

  private record GeminiResponse(List<Candidate> candidates) {}

  private record Candidate(Content content) {}

  private record Content(List<Part> parts) {}

  private record Part(String text) {}
}
