package com.bookshelves.domain.ai.client;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.util.ArrayList;
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

// 모임 대화를 분석 축 3개에 맞춰 주제 3개로 요약한다.
//
// 질문 생성과 목적·프롬프트·검증이 전부 달라 GeminiQuestionClient와 합치지 않는다. 다만 호출 규약은
// 공유한다 — 같은 설정 키를 쓰고, 경로를 템플릿으로 넘기고, 세대별로 generationConfig를 가른다.
//
// 발화자는 익명 라벨로 치환해 넣는다. 닉네임을 넣으면 모델이 요약에 그대로 옮길 수 있고, 명세는 요약에
// 개인 닉네임 미노출과 집단·중립 표현을 요구한다. 라벨은 회원 단위로 일관되게 붙여 의견 대립을 추적할
// 수 있게 한다.
@Slf4j
@Component
public class GeminiSummaryClient {

  private static final String API_KEY_HEADER = "x-goog-api-key";
  private static final double LEGACY_TEMPERATURE = 0.2;
  private static final String THINKING_LEVEL = "high";

  // title 컬럼이 VARCHAR(255)다. 초과분이 저장 단계까지 가면 INSERT가 실패하므로 여기서 거른다.
  static final int MAX_TITLE_LENGTH = 60;

  // 대화량은 모임 duration 60분·정원 6명·메시지 500자로 구조상 상한이 있으나, 생성 API 검증에
  // 최대값이 없어 명세 밖의 값이 들어올 여지가 있다. 프롬프트 자체에도 상한을 둔다.
  private static final int MAX_MESSAGES = 400;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  @Autowired
  public GeminiSummaryClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${external.gemini.api-key}") String apiKey,
      @Value("${external.gemini.model:" + GeminiQuestionClient.DEFAULT_MODEL + "}") String model) {
    this(buildRestClient(restClientBuilder), objectMapper, apiKey, model);
  }

  GeminiSummaryClient(
      RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.model = model;
  }

  private static RestClient buildRestClient(RestClient.Builder restClientBuilder) {
    // 대화 전체가 입력이라 질문 생성보다 응답이 오래 걸린다. 요약은 종료 후 비동기 작업이라
    // 지연 상한에 여유가 있다.
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(10));
    requestFactory.setReadTimeout(Duration.ofSeconds(120));

    return restClientBuilder
        .baseUrl(GeminiQuestionClient.BASE_URL)
        .requestFactory(requestFactory)
        .build();
  }

  /** 축별 요약 초안. 검증을 통과한 축만 담긴다. 빠진 축은 호출부가 안내 문구로 채운다. */
  public record SummaryDraft(String title, String content) {}

  public Map<SummaryAxis, SummaryDraft> generateSummaries(
      Book book, List<AIQuestion> questions, List<ChatMessage> messages) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다.");
    }

    GeminiResponse response =
        restClient
            .post()
            .uri(GeminiQuestionClient.GENERATE_CONTENT_PATH, model)
            .header(API_KEY_HEADER, apiKey)
            .body(GeminiRequest.of(buildPrompt(book, questions, messages), generationConfig()))
            .retrieve()
            .body(GeminiResponse.class);

    String json = extractText(response);
    if (json == null || json.isBlank()) {
      throw new IllegalStateException("Gemini 응답에서 요약을 추출하지 못했습니다.");
    }
    log.debug("Gemini 요약 원문 응답: model={}, body={}", model, json);
    return validate(parse(json));
  }

  String buildPrompt(Book book, List<AIQuestion> questions, List<ChatMessage> messages) {
    StringBuilder prompt = new StringBuilder();
    prompt
        .append("당신은 독서 모임의 기록자입니다. 아래 모임의 대화를 세 가지 주제로 요약해 주세요.\n\n")
        .append("[책 정보]\n제목: ")
        .append(book.getTitle())
        .append('\n');
    appendIfPresent(prompt, "저자", book.getAuthor());
    appendIfPresent(prompt, "출판사", book.getPublisher());
    appendIfPresent(
        prompt, "출간일", book.getPublishedDate() == null ? null : book.getPublishedDate().toString());
    appendIfPresent(prompt, "ISBN", book.getIsbn());
    appendIfPresent(prompt, "분류", book.getKdcName());
    appendIfPresent(prompt, "소개", book.getDescription());

    prompt.append("\n[발제 질문]\n");
    questions.forEach(
        question ->
            prompt
                .append(question.getQuestionOrder())
                .append(". ")
                .append(question.getContent())
                .append('\n'));

    prompt.append("\n[대화]\n").append(anonymize(messages));

    prompt.append("\n[주제 축] 세 주제는 아래 축을 하나씩 반영합니다.\n");
    SummaryAxis.ordered()
        .forEach(
            axis ->
                prompt
                    .append(axis.getDisplayOrder())
                    .append(". ")
                    .append(axis.name())
                    .append(" — ")
                    .append(axis.getDescription())
                    .append('\n'));

    prompt
        .append("\n[규칙]\n")
        .append("1. 책에 없는 사실이나 대화에서 언급되지 않은 내용은 만들지 마세요.\n")
        .append("2. 참여자 간 의견이 다르면 하나로 단정하지 말고 차이를 함께 서술하세요.\n")
        .append("3. 제목은 대화 내용을 압축한 질문형 또는 핵심 문장형으로 쓰세요.\n")
        .append("4. 요약은 참여자들의 주요 의견을 2~3문장으로 객관적으로 정리하세요.\n")
        .append("5. 특정 참여자를 지목하지 마세요. 참여자 A 같은 라벨을 결과에 쓰지 말고 ")
        .append("집단·중립 표현으로 쓰세요.\n")
        .append("6. 제목은 ")
        .append(MAX_TITLE_LENGTH)
        .append("자를 넘기지 마세요.\n")
        .append("7. 해당 축으로 요약할 대화가 없으면 summary를 빈 문자열로 두세요.\n\n")
        .append("[출력 형식] 다른 설명 없이 아래 JSON 배열만 출력하세요. 반드시 3개입니다.\n")
        .append("[{\"axis\": \"KEY_ARGUMENT\", \"title\": \"...\", \"summary\": \"...\"}, ...]");
    return prompt.toString();
  }

  // 회원 단위로 A, B, C… 라벨을 붙인다. 같은 회원은 대화 내내 같은 라벨을 유지해야 누가 누구에게
  // 동의하거나 반박했는지 추적된다. 라벨은 프롬프트에만 쓰고 저장하지 않는다.
  private String anonymize(List<ChatMessage> messages) {
    Map<Long, String> labelsByMember = new LinkedHashMap<>();
    StringBuilder conversation = new StringBuilder();

    List<ChatMessage> bounded =
        messages.size() > MAX_MESSAGES
            ? messages.subList(messages.size() - MAX_MESSAGES, messages.size())
            : messages;
    if (bounded.size() < messages.size()) {
      log.warn("요약 입력이 상한을 넘어 앞부분을 잘랐다: 전체={}, 사용={}", messages.size(), bounded.size());
    }

    for (ChatMessage message : bounded) {
      Long memberId = message.getSenderMember().getId();
      String label =
          labelsByMember.computeIfAbsent(
              memberId, key -> "참여자 " + (char) ('A' + labelsByMember.size()));
      conversation.append(label).append(": ").append(message.getMessage()).append('\n');
    }
    return conversation.toString();
  }

  private void appendIfPresent(StringBuilder prompt, String label, String value) {
    if (value != null && !value.isBlank()) {
      prompt.append(label).append(": ").append(value.strip()).append('\n');
    }
  }

  private List<GeneratedSummary> parse(String json) {
    try {
      return objectMapper.readValue(stripCodeFence(json), new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalStateException("Gemini 요약 응답 JSON 파싱에 실패했습니다.", e);
    }
  }

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

  // 구조만 검증한다. 내용이 실제 대화에 근거했는지는 판정할 수단이 없다.
  // 제목 길이는 예외다 — DB 컬럼 제약과 직결되어 여기서 거르지 않으면 저장 단계에서 실패한다.
  private Map<SummaryAxis, SummaryDraft> validate(List<GeneratedSummary> candidates) {
    Map<SummaryAxis, SummaryDraft> accepted = new LinkedHashMap<>();
    for (GeneratedSummary candidate : candidates) {
      if (candidate == null) {
        continue;
      }
      SummaryAxis axis = toAxis(candidate.axis());
      if (axis == null || accepted.containsKey(axis)) {
        continue; // 모르는 축이거나 중복 — 먼저 온 것만 채택
      }
      String title = normalize(candidate.title());
      if (title == null || title.length() > MAX_TITLE_LENGTH) {
        log.warn(
            "요약 제목이 검증에 걸려 안내 문구를 사용한다: axis={}, 길이={}, 허용={}",
            axis,
            title == null ? 0 : title.length(),
            MAX_TITLE_LENGTH);
        continue;
      }
      accepted.put(axis, new SummaryDraft(title, normalize(candidate.summary())));
    }
    return accepted;
  }

  private SummaryAxis toAxis(String raw) {
    if (raw == null) {
      return null;
    }
    try {
      return SummaryAxis.valueOf(raw.strip());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String normalize(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    return text.strip().replaceAll("\\s+", " ");
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

  // 아는 구세대만 제외한다. 3.x만 검사하면 이후 세대에서 질문 클라이언트와 다른 요청을 보내게 된다.
  private boolean supportsThinkingLevel() {
    return !model.startsWith("gemini-1.") && !model.startsWith("gemini-2.");
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record GeneratedSummary(String axis, String title, String summary) {}

  private record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {
    private static GeminiRequest of(String prompt, GenerationConfig generationConfig) {
      List<Content> contents = new ArrayList<>();
      contents.add(new Content(List.of(new Part(prompt))));
      return new GeminiRequest(contents, generationConfig);
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private record GenerationConfig(
      String responseMimeType, Double temperature, ThinkingConfig thinkingConfig) {}

  private record ThinkingConfig(String thinkingLevel) {}

  private record GeminiResponse(List<Candidate> candidates) {}

  private record Candidate(Content content) {}

  private record Content(List<Part> parts) {}

  private record Part(String text) {}
}
