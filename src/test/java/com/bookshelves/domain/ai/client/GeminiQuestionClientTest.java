package com.bookshelves.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.book.entity.Book;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiQuestionClientTest {

  private static final String DESCRIPTION =
      "선천적으로 감정을 느끼지 못하는 소년 윤재가 세상과 부딪히며 조금씩 변해가는 이야기를 담은 성장소설이다.";

  private MockRestServiceServer mockServer;
  private GeminiQuestionClient geminiQuestionClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    geminiQuestionClient =
        new GeminiQuestionClient(
            restClientBuilder.baseUrl(GeminiQuestionClient.BASE_URL).build(),
            new ObjectMapper(),
            "test-api-key");
  }

  private Book bookWithDescription(String description) {
    return Book.builder()
        .isbn("9788936434595")
        .title("아몬드")
        .author("손원평")
        .publisher("창비")
        .publishedDate(LocalDate.of(2017, 3, 31))
        .kdcName("한국소설")
        .description(description)
        .build();
  }

  private void respondWith(String modelText) {
    // baseUrl이 실제로 결합됐는지까지 검증한다 — 모델명을 문자열로 이어붙이면 콜론이 스킴으로 파싱돼
    // baseUrl이 무시되는데, 상대 경로로 매칭하면 그 상태도 통과해버린다
    mockServer
        .expect(
            requestTo(
                GeminiQuestionClient.BASE_URL
                    + "/models/"
                    + GeminiQuestionClient.DEFAULT_MODEL
                    + ":generateContent"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("x-goog-api-key", "test-api-key"))
        .andRespond(
            withSuccess(
                """
                {"candidates":[{"content":{"parts":[{"text":%s}]}}]}
                """
                    .formatted(new ObjectMapper().valueToTree(modelText).toString()),
                MediaType.APPLICATION_JSON));
  }

  @Test
  void returnsGeneratedQuestionsByOrder() {
    respondWith(
        """
        [{"order":1,"question":"윤재를 보는 시선이 읽기 전과 후로 어떻게 달라졌나요?"},
         {"order":3,"question":"윤재가 가장 크게 흔들린 장면은 어디였나요?"}]
        """);

    Map<Integer, String> generated =
        geminiQuestionClient.generateQuestions(bookWithDescription(DESCRIPTION));

    // 응답에 없는 순서는 호출부가 시드 원문으로 채운다
    assertThat(generated).containsOnlyKeys(1, 3);
    assertThat(generated.get(1)).isEqualTo("윤재를 보는 시선이 읽기 전과 후로 어떻게 달라졌나요?");
    mockServer.verify();
  }

  @Test
  void callsEvenWhenDescriptionIsMissing() {
    // 모델 지식 사용을 허용하므로 소개가 없어도 호출한다 — 제목·저자만으로 알아볼 수 있다
    respondWith(
        """
        [{"order":5,"question":"이 책을 한 문장으로 소개한다면?"}]
        """);

    assertThat(geminiQuestionClient.generateQuestions(bookWithDescription(null)))
        .containsOnlyKeys(5);
    mockServer.verify();
  }

  @Test
  void dropsQuestionThatExceedsAbsoluteLengthCap() {
    // 시드 길이 대비 비율이 아니라 절대 상한(100자)으로 잰다
    respondWith(
        """
        [{"order":3,"question":"%s"},
         {"order":4,"question":"짧은 쪽은 남는다. 이 문장은 상한 안에 들어온다."}]
        """
            .formatted("가".repeat(101)));

    assertThat(geminiQuestionClient.generateQuestions(bookWithDescription(DESCRIPTION)))
        .containsOnlyKeys(4);
  }

  @Test
  void dropsUnknownOrderAndDuplicateOrder() {
    respondWith(
        """
        [{"order":99,"question":"모르는 순서라 버려진다"},
         {"order":4,"question":"먼저 온 4번이 채택된다"},
         {"order":4,"question":"나중에 온 4번은 무시된다"}]
        """);

    Map<Integer, String> generated =
        geminiQuestionClient.generateQuestions(bookWithDescription(DESCRIPTION));

    assertThat(generated).containsExactly(Map.entry(4, "먼저 온 4번이 채택된다"));
  }

  @Test
  void stripsCodeFenceBeforeParsing() {
    respondWith(
        """
        ```json
        [{"order":5,"question":"감정을 배우는 소년의 이야기라고 소개하면 어떨까요?"}]
        ```
        """);

    assertThat(geminiQuestionClient.generateQuestions(bookWithDescription(DESCRIPTION)))
        .containsOnlyKeys(5);
  }

  @Test
  void throwsWhenResponseIsNotParseable() {
    respondWith("전혀 JSON이 아닌 응답");

    assertThatThrownBy(
            () -> geminiQuestionClient.generateQuestions(bookWithDescription(DESCRIPTION)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void throwsWhenApiKeyIsMissing() {
    GeminiQuestionClient clientWithoutKey =
        new GeminiQuestionClient(RestClient.builder().build(), new ObjectMapper(), "");

    assertThatThrownBy(() -> clientWithoutKey.generateQuestions(bookWithDescription(DESCRIPTION)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GEMINI_API_KEY");
  }

  // Gemini 3.x는 temperature를 기본값(1.0)에서 낮추면 추론이 흐트러진다고 구글이 명시한다.
  // 세대별로 다른 필드를 보내는 것이 이 클라이언트의 조용한 분기점이라 요청 본문까지 검증한다.
  private void expectGenerationConfig(String model, RequestMatcher configMatcher) {
    mockServer
        .expect(requestTo(GeminiQuestionClient.BASE_URL + "/models/" + model + ":generateContent"))
        .andExpect(configMatcher)
        .andRespond(
            withSuccess(
                """
                {"candidates":[{"content":{"parts":[{"text":"[{\\"order\\":1,\\"question\\":\\"충분히 긴 질문 문장입니다.\\"}]"}]}}]}
                """,
                MediaType.APPLICATION_JSON));
  }

  private GeminiQuestionClient clientForModel(String model, RestClient.Builder builder) {
    return new GeminiQuestionClient(
        builder.baseUrl(GeminiQuestionClient.BASE_URL).build(),
        new ObjectMapper(),
        "test-api-key",
        model);
  }

  @Test
  void sendsThinkingLevelInsteadOfTemperatureOnGemini3() {
    expectGenerationConfig(
        GeminiQuestionClient.DEFAULT_MODEL,
        jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("high"));

    geminiQuestionClient.generateQuestions(bookWithDescription(DESCRIPTION));
    mockServer.verify();
  }

  @Test
  void sendsTemperatureWithoutThinkingLevelOnGemini2() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    expectGenerationConfig(
        "gemini-2.0-flash", jsonPath("$.generationConfig.temperature").value(0.2));

    clientForModel("gemini-2.0-flash", builder).generateQuestions(bookWithDescription(DESCRIPTION));
    mockServer.verify();
  }

  @Test
  void promptCarriesIdentifiersButNotSeedSentences() {
    String prompt = geminiQuestionClient.buildPrompt(bookWithDescription(DESCRIPTION));

    // 동명이서를 가르는 단서가 반드시 들어가야 한다
    assertThat(prompt).contains("9788936434595").contains("2017-03-31");

    // 시드 원문이 들어가면 모델이 그 문장을 보존하려 들면서 수식어만 붙이는 결과로 수렴한다.
    // 자리의 의도만 전달돼야 한다.
    SeedQuestion.ordered()
        .forEach(
            seed -> {
              assertThat(prompt).doesNotContain(seed.getContent());
              assertThat(prompt).contains(seed.getIntent());
            });
  }
}
