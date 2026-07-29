package com.bookshelves.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.book.entity.Book;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
        .kdcName("한국소설")
        .description(description)
        .build();
  }

  private void respondWith(String modelText) {
    // MockRestServiceServer는 baseUrl이 붙기 전 단계에서 가로채므로 상대 경로로 매칭한다
    mockServer
        .expect(requestTo("gemini-2.0-flash:generateContent"))
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
  void returnsOnlyAdaptedQuestions() {
    respondWith(
        """
        [{"order":1,"adapted":true,"question":"아몬드를 읽기 전과 후, 윤재를 보는 시선이 어떻게 달라졌나요?"},
         {"order":2,"adapted":false,"question":"원문 그대로"},
         {"order":3,"adapted":true,"question":"윤재가 가장 크게 흔들린 장면은 어디였나요?"}]
        """);

    Map<Integer, String> adapted =
        geminiQuestionClient.adaptSeedQuestions(bookWithDescription(DESCRIPTION));

    // adapted=false인 2번은 빠지고, 나머지 순서는 호출부가 시드 원문으로 채운다
    assertThat(adapted).containsOnlyKeys(1, 3);
    assertThat(adapted.get(1)).isEqualTo("아몬드를 읽기 전과 후, 윤재를 보는 시선이 어떻게 달라졌나요?");
    mockServer.verify();
  }

  @Test
  void skipsCallWhenDescriptionIsTooShort() {
    // 각색 근거가 없으면 호출 자체를 하지 않는다 — mockServer에 기대를 걸지 않았으므로 호출 시 실패한다
    assertThat(geminiQuestionClient.adaptSeedQuestions(bookWithDescription("짧은 소개"))).isEmpty();
    assertThat(geminiQuestionClient.adaptSeedQuestions(bookWithDescription(null))).isEmpty();
    mockServer.verify();
  }

  @Test
  void dropsQuestionThatIsTooLong() {
    String seed = SeedQuestion.MEMORABLE_SCENE.getContent();
    respondWith(
        """
        [{"order":3,"adapted":true,"question":"%s"}]
        """
            .formatted("가".repeat(seed.length() * 2 + 1)));

    assertThat(geminiQuestionClient.adaptSeedQuestions(bookWithDescription(DESCRIPTION))).isEmpty();
  }

  @Test
  void dropsUnknownOrderAndDuplicateOrder() {
    respondWith(
        """
        [{"order":99,"adapted":true,"question":"모르는 순서라 버려진다"},
         {"order":4,"adapted":true,"question":"먼저 온 4번이 채택된다"},
         {"order":4,"adapted":true,"question":"나중에 온 4번은 무시된다"}]
        """);

    Map<Integer, String> adapted =
        geminiQuestionClient.adaptSeedQuestions(bookWithDescription(DESCRIPTION));

    assertThat(adapted).containsExactly(Map.entry(4, "먼저 온 4번이 채택된다"));
  }

  @Test
  void stripsCodeFenceBeforeParsing() {
    respondWith(
        """
        ```json
        [{"order":5,"adapted":true,"question":"감정을 배우는 소년의 이야기라고 소개하면 어떨까요?"}]
        ```
        """);

    assertThat(geminiQuestionClient.adaptSeedQuestions(bookWithDescription(DESCRIPTION)))
        .containsOnlyKeys(5);
  }

  @Test
  void throwsWhenResponseIsNotParseable() {
    respondWith("전혀 JSON이 아닌 응답");

    assertThatThrownBy(
            () -> geminiQuestionClient.adaptSeedQuestions(bookWithDescription(DESCRIPTION)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void throwsWhenApiKeyIsMissing() {
    GeminiQuestionClient clientWithoutKey =
        new GeminiQuestionClient(RestClient.builder().build(), new ObjectMapper(), "");

    assertThatThrownBy(() -> clientWithoutKey.adaptSeedQuestions(bookWithDescription(DESCRIPTION)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GEMINI_API_KEY");
  }
}
