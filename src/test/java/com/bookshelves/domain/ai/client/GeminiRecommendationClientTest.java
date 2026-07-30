package com.bookshelves.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.book.entity.Book;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiRecommendationClientTest {

  private MockRestServiceServer mockServer;
  private GeminiRecommendationClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    client =
        new GeminiRecommendationClient(
            new GeminiClient(
                builder.baseUrl(GeminiClient.BASE_URL).build(),
                new ObjectMapper(),
                "test-api-key",
                GeminiClient.DEFAULT_MODEL));
  }

  private Book book() {
    return Book.builder()
        .isbn("9788936434595")
        .title("아몬드")
        .author("손원평")
        .publisher("창비")
        .publishedDate(LocalDate.of(2017, 3, 31))
        .kdcName("한국소설")
        .description("감정을 느끼지 못하는 소년 윤재가 세상과 부딪히며 변해가는 성장소설이다.")
        .build();
  }

  private void respondWith(String modelText) {
    GeminiTestResponses.expectPost(mockServer, modelText);
  }

  @Test
  void returnsGeneratedMessage() {
    respondWith(
        """
        [{"message":"감정을 배우는 소년의 조용한 성장 기록"}]
        """);

    assertThat(client.generateMessage(book())).isEqualTo("감정을 배우는 소년의 조용한 성장 기록");
    mockServer.verify();
  }

  @Test
  void returnsNullWhenMessageIsTooLong() {
    // 폴백 여부를 호출부가 판단할 수 있도록 검증 실패는 예외가 아니라 null로 알린다
    respondWith(
        """
        [{"message":"%s"}]
        """
            .formatted("가".repeat(41)));

    assertThat(client.generateMessage(book())).isNull();
  }

  @Test
  void returnsNullWhenMessageIsTooShort() {
    respondWith(
        """
        [{"message":"좋은 책"}]
        """);

    assertThat(client.generateMessage(book())).isNull();
  }

  @Test
  void acceptsMessageThatSlightlyOvershootsThePromptRange() {
    // 프롬프트는 30자를 요구하지만 모델은 글자를 셀 수 없다. 요구 범위를 하드 컷으로 쓰면
    // 멀쩡한 문장이 몇 글자 차이로 버려지고 폴백만 늘어난다.
    String thirtyFive = "가".repeat(35);
    respondWith(
        """
        [{"message":"%s"}]
        """
            .formatted(thirtyFive));

    assertThat(client.generateMessage(book())).isEqualTo(thirtyFive);
  }

  @Test
  void promptForbidsInventingContentAndRepeatingTitle() {
    String prompt = client.buildPrompt(book());

    assertThat(prompt).contains("지어내지 마세요");
    assertThat(prompt).contains("책 제목을 그대로 반복하지 마세요");
    // 동명이서를 가르는 단서는 여기서도 필요하다
    assertThat(prompt).contains("9788936434595");
  }
}
