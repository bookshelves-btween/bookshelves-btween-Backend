package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.book.entity.Book;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// 모임 질문 다섯 개를 그 책에 맞게 새로 쓴다.
//
// 예전에는 시드 원문을 프롬프트에 주고 각색시켰다. 그러면 모델이 원문을 보존하려 들면서 앞에 수식어만
// 붙이는 결과로 수렴한다. 감상을 묻는 질문 앞에 줄거리 요약을 덧대는 식이라 원문보다 길기만 하고
// 나아진 게 없다. 그래서 시드 문장은 프롬프트에서 빼고, 자리의 의도(SeedQuestion.intent)만 주고
// 문장은 처음부터 쓰게 한다.
//
// 모델의 내부 지식 사용을 허용한다. 대신 아는 척을 금지한다 — 확실하지 않으면 책의 구체적 내용을 아예
// 언급하지 말고 담백한 질문을 쓰게 한다. 어설프게 아는 내용을 끼워넣는 것이 가장 나쁜 결과이기 때문이다.
// 동명이서를 가르도록 ISBN과 출간일까지 프롬프트에 넣는다.
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
  static final String DEFAULT_MODEL = "gemini-3.6-flash";
  private static final String API_KEY_HEADER = "x-goog-api-key";

  // 2.x 이하는 thinking이 없어 낮은 temperature로 변형 폭을 좁힌다.
  //
  // 3.x부터는 반대다 — 추론이 기본 temperature(1.0)에 맞춰 조정돼 있어 낮추면 논리가 오히려 흐트러지고
  // 사고 루프에 빠질 수 있다고 구글이 명시한다. 그래서 3.x에서는 temperature를 아예 보내지 않고
  // thinkingLevel로 사고량을 올린다. 질문 생성은 준비 단계(비동기)라 지연보다 규칙 준수가 중요하다.
  private static final double LEGACY_TEMPERATURE = 0.2;
  private static final String THINKING_LEVEL = "high";

  // 길이는 시드 대비 비율이 아니라 절대값으로 잰다.
  //
  // 문장을 새로 쓰게 한 이상 시드 길이는 기준이 될 수 없다. 비율로 재던 시절에는 시드가 28자로 짧은 1번에서
  // 멀쩡한 질문이 한 글자 차이로 잘려나갔다. MAX_LENGTH는 프롬프트에 적는 상한과 같은 값을 쓴다.
  private static final int MAX_LENGTH = 100;
  private static final int MIN_LENGTH = 10;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  @Autowired
  public GeminiQuestionClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${external.gemini.api-key}") String apiKey,
      @Value("${external.gemini.model:" + DEFAULT_MODEL + "}") String model) {
    this(buildRestClient(restClientBuilder), objectMapper, apiKey, model);
  }

  GeminiQuestionClient(RestClient restClient, ObjectMapper objectMapper, String apiKey) {
    this(restClient, objectMapper, apiKey, DEFAULT_MODEL);
  }

  GeminiQuestionClient(
      RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.model = model;
  }

  private static RestClient buildRestClient(RestClient.Builder restClientBuilder) {
    // LLM 응답 지연이 준비 스레드를 계속 붙잡지 않도록 타임아웃을 건다 — 초과 시 시드 원문으로 진행
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(20));

    return restClientBuilder.baseUrl(BASE_URL).requestFactory(requestFactory).build();
  }

  /**
   * 질문 다섯 개를 LLM 1회 호출로 생성한다.
   *
   * <p>소개가 비어 있어도 호출한다. 모델이 제목·저자로 책을 알아볼 수 있고, 못 알아보더라도 자리의 의도만으로 시드보다 나은 문장이 나올 수 있다.
   *
   * @return question_order → 생성된 질문. 검증을 통과한 항목만 담긴다.
   */
  public Map<Integer, String> generateQuestions(Book book) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
    }

    GeminiResponse response =
        restClient
            .post()
            .uri(GENERATE_CONTENT_PATH, model)
            .header(API_KEY_HEADER, apiKey)
            .body(GeminiRequest.of(buildPrompt(book), generationConfig()))
            .retrieve()
            .body(GeminiResponse.class);

    String json = extractText(response);
    if (json == null || json.isBlank()) {
      throw new IllegalStateException("Gemini 응답에서 질문을 추출하지 못했습니다.");
    }
    // 검증에서 무엇이 왜 떨어졌는지는 채택 결과만 봐서는 알 수 없다 — 원문을 남겨 대조 가능하게 한다
    log.debug("Gemini 원문 응답: model={}, body={}", model, json);
    return validate(parse(json));
  }

  String buildPrompt(Book book) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("당신은 독서 모임 진행자입니다. 아래 책으로 나눌 대화 질문 5개를 만들어 주세요.\n\n")
        .append("[책 정보]\n")
        .append("제목: ")
        .append(book.getTitle())
        .append('\n');
    appendIfPresent(prompt, "저자", book.getAuthor());
    appendIfPresent(prompt, "출판사", book.getPublisher());
    // 출간일과 ISBN은 동명이서·개정판을 가르는 유일한 단서다 — 모델 지식을 허용한 이상 반드시 넣는다
    appendIfPresent(
        prompt, "출간일", book.getPublishedDate() == null ? null : book.getPublishedDate().toString());
    appendIfPresent(prompt, "ISBN", book.getIsbn());
    appendIfPresent(prompt, "분류", book.getKdcName());
    appendIfPresent(prompt, "소개", book.getDescription());

    // 시드 원문이 아니라 자리의 의도만 준다 — 원문을 보여주면 모델이 그 문장을 보존하려 든다
    prompt.append("\n[질문 자리] 순서와 의도는 고정입니다. 문장은 이 책에 맞게 처음부터 새로 쓰세요.\n");
    SeedQuestion.ordered()
        .forEach(
            seed ->
                prompt
                    .append(seed.getQuestionOrder())
                    .append(". ")
                    .append(seed.getIntent())
                    .append('\n'));

    prompt
        .append("\n[규칙]\n")
        .append("1. 이 책을 확실히 안다면 실제 인물·사건·주제를 끌어와 구체적으로 물으세요. ")
        .append("확실하지 않다면 책의 구체적인 내용을 아예 언급하지 말고, ")
        .append("이 책에도 자연스럽게 들어맞는 담백한 질문으로 쓰세요. ")
        .append("어설프게 아는 내용을 끼워넣는 것이 가장 나쁩니다.\n")
        .append("2. 참가자가 자기 해석과 경험을 말할 수 있는 열린 질문으로 쓰세요. ")
        .append("정답이 정해진 퀴즈나 줄거리 확인 질문은 만들지 마세요.\n")
        .append("3. 줄거리를 요약해 알려주지 마세요. 질문이지 설명이 아닙니다.\n")
        .append("4. \"~를 다룬 이 작품을 읽고\", \"~한 이야기를 담은 이 책에서\"처럼 ")
        .append("수식어를 앞에 붙이는 방식으로 쓰지 마세요.\n")
        .append("5. 한국어로, 각 질문은 한두 문장이며 ")
        .append(MAX_LENGTH)
        .append("자를 넘기지 마세요.\n\n")
        .append("[출력 형식] 다른 설명 없이 아래 JSON 배열만 출력하세요.\n")
        .append("[{\"order\": 1, \"question\": \"...\"}, ...]");
    return prompt.toString();
  }

  private void appendIfPresent(StringBuilder prompt, String label, String value) {
    if (value != null && !value.isBlank()) {
      prompt.append(label).append(": ").append(value.strip()).append('\n');
    }
  }

  private List<GeneratedQuestion> parse(String json) {
    try {
      return objectMapper.readValue(stripCodeFence(json), new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Gemini 질문 생성 응답 JSON 파싱에 실패했습니다.", e);
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

  // LLM 응답을 그대로 믿지 않는다 — 구조를 어긴 항목은 버리고 호출부가 시드 원문을 쓰게 한다.
  //
  // 내용 검증은 하지 않는다. 모델 지식 사용을 허용한 이상 책에 실제로 있는 내용인지를 우리가 판정할
  // 수단이 없고, 흉내만 낸 검증은 통과시키면 안 되는 것을 통과시키면서 멀쩡한 것만 떨어뜨린다.
  private Map<Integer, String> validate(List<GeneratedQuestion> candidates) {
    Set<Integer> knownOrders = SeedQuestion.allOrders();

    Map<Integer, String> accepted = new LinkedHashMap<>();
    for (GeneratedQuestion candidate : candidates) {
      if (candidate == null || candidate.order() == null) {
        continue;
      }
      if (!knownOrders.contains(candidate.order()) || accepted.containsKey(candidate.order())) {
        continue; // 모르는 순서이거나 중복 — 먼저 온 것만 채택
      }
      String question = normalize(candidate.question());
      if (question == null || question.length() < MIN_LENGTH || question.length() > MAX_LENGTH) {
        // 버린 문장까지 남긴다 — 길이 상한이 실제로 어디서 걸리는지 로그만 보고 판단할 수 있어야 한다
        log.warn(
            "생성 질문이 길이 검증에 걸려 시드 원문을 사용한다: order={}, 길이={}, 허용={}, 질문={}",
            candidate.order(),
            question == null ? 0 : question.length(),
            MAX_LENGTH,
            question);
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
  // 필드 하나 때문에 질문 5개가 전부 시드 원문으로 떨어지는 것을 막는다
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GeneratedQuestion(Integer order, String question) {}

  private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    private static GeminiRequest of(String prompt, GenerationConfig generationConfig) {
      List<Content> contents = new ArrayList<>();
      contents.add(new Content(List.of(new Part(prompt))));
      return new GeminiRequest(contents, generationConfig);
    }
  }

  // JSON 강제는 세대 공통, 변형 폭을 좁히는 수단만 세대별로 다르다.
  private GenerationConfig generationConfig() {
    return supportsThinkingLevel()
        ? new GenerationConfig("application/json", null, new ThinkingConfig(THINKING_LEVEL))
        : new GenerationConfig("application/json", LEGACY_TEMPERATURE, null);
  }

  // 3.x 이후를 thinking 계열로 본다 — 아는 구세대만 제외해야 새 모델이 조용히 구설정으로 떨어지지 않는다
  private boolean supportsThinkingLevel() {
    return !model.startsWith("gemini-1.") && !model.startsWith("gemini-2.");
  }

  // 세대별로 안 쓰는 필드는 아예 보내지 않는다 — null이 그대로 직렬화되면 API가 400으로 거절한다
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record GenerationConfig(
      String responseMimeType, Double temperature, ThinkingConfig thinkingConfig) {}

  private record ThinkingConfig(String thinkingLevel) {}

  private record GeminiResponse(List<Candidate> candidates) {}

  private record Candidate(Content content) {}

  private record Content(List<Part> parts) {}

  private record Part(String text) {}
}
