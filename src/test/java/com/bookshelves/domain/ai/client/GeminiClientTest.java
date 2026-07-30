package com.bookshelves.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// 용도와 무관한 호출 규약만 검증한다. 프롬프트와 응답 검증은 각 용도별 클라이언트 테스트가 맡는다.
class GeminiClientTest {

  private record Item(Integer order, String text) {}

  private MockRestServiceServer mockServer;
  private GeminiClient geminiClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    geminiClient = clientOn(builder, "test-api-key", GeminiClient.DEFAULT_MODEL);
  }

  private GeminiClient clientOn(RestClient.Builder builder, String apiKey, String model) {
    return new GeminiClient(
        builder.baseUrl(GeminiClient.BASE_URL).build(), new ObjectMapper(), apiKey, model);
  }

  private List<Item> generate() {
    return geminiClient.generate("프롬프트", new TypeReference<>() {});
  }

  private void respondWith(String modelText) {
    // baseUrl이 실제로 결합됐는지까지 검증한다. 모델명을 문자열로 이어붙이면 콜론이 스킴으로 파싱돼
    // baseUrl이 무시되는데, 상대 경로로 매칭하면 그 상태도 통과해버린다.
    mockServer
        .expect(
            requestTo(
                GeminiClient.BASE_URL
                    + "/models/"
                    + GeminiClient.DEFAULT_MODEL
                    + ":generateContent"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("x-goog-api-key", "test-api-key"))
        .andRespond(withSuccess(GeminiTestResponses.wrap(modelText), MediaType.APPLICATION_JSON));
  }

  @Test
  void sendsToAbsoluteModelUrlAndParsesResponse() {
    respondWith(
        """
        [{"order":1,"text":"첫 항목"}]
        """);

    assertThat(generate()).containsExactly(new Item(1, "첫 항목"));
    mockServer.verify();
  }

  @Test
  void stripsCodeFenceBeforeParsing() {
    respondWith(
        """
        ```json
        [{"order":1,"text":"펜스가 붙어도 파싱된다"}]
        ```
        """);

    assertThat(generate()).containsExactly(new Item(1, "펜스가 붙어도 파싱된다"));
  }

  @Test
  void throwsWhenResponseIsNotParseable() {
    respondWith("전혀 JSON이 아닌 응답");

    assertThatThrownBy(this::generate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("파싱");
  }

  @Test
  void throwsWhenResponseCarriesNoText() {
    mockServer
        .expect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
            {"candidates":[]}
            """,
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(this::generate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("추출");
  }

  @Test
  void throwsWhenApiKeyIsMissing() {
    GeminiClient clientWithoutKey = clientOn(RestClient.builder(), "", GeminiClient.DEFAULT_MODEL);

    assertThatThrownBy(() -> clientWithoutKey.generate("프롬프트", new TypeReference<List<Item>>() {}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GEMINI_API_KEY");
  }

  // Gemini 3.x는 temperature를 기본값(1.0)에서 낮추면 추론이 흐트러진다고 구글이 명시한다.
  // 세대별로 다른 필드를 보내는 것이 이 클라이언트의 조용한 분기점이라 요청 본문까지 검증한다.
  private void expectGenerationConfig(String model, RequestMatcher configMatcher) {
    mockServer
        .expect(requestTo(GeminiClient.BASE_URL + "/models/" + model + ":generateContent"))
        .andExpect(configMatcher)
        .andRespond(
            withSuccess(
                """
                {"candidates":[{"content":{"parts":[{"text":"[{\\"order\\":1,\\"text\\":\\"본문\\"}]"}]}}]}
                """,
                MediaType.APPLICATION_JSON));
  }

  @Test
  void sendsThinkingLevelInsteadOfTemperatureOnGemini3() {
    expectGenerationConfig(
        GeminiClient.DEFAULT_MODEL,
        jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("high"));

    generate();
    mockServer.verify();
  }

  @Test
  void sendsTemperatureWithoutThinkingLevelOnGemini2() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    expectGenerationConfig(
        "gemini-2.0-flash", jsonPath("$.generationConfig.temperature").value(0.2));

    clientOn(builder, "test-api-key", "gemini-2.0-flash")
        .generate("프롬프트", new TypeReference<List<Item>>() {});
    mockServer.verify();
  }
}
