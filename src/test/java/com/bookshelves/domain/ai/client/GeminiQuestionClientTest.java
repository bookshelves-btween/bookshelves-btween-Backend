package com.bookshelves.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
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
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// 프롬프트 구성과 생성 결과 검증만 다룬다. URL·헤더·세대별 설정 같은 호출 규약은 GeminiClientTest가 맡는다.
class GeminiQuestionClientTest {

  private static final String DESCRIPTION =
      "선천적으로 감정을 느끼지 못하는 소년 윤재가 세상과 부딪히며 조금씩 변해가는 이야기를 담은 성장소설이다.";

  private MockRestServiceServer mockServer;
  private GeminiQuestionClient geminiQuestionClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    geminiQuestionClient =
        new GeminiQuestionClient(
            new GeminiClient(
                builder.baseUrl(GeminiClient.BASE_URL).build(),
                new ObjectMapper(),
                "test-api-key",
                GeminiClient.DEFAULT_MODEL));
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
    mockServer
        .expect(method(HttpMethod.POST))
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
    // 모델 지식 사용을 허용하므로 소개가 없어도 호출한다. 제목·저자만으로 알아볼 수 있다.
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
