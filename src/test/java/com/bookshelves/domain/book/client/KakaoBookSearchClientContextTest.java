package com.bookshelves.domain.book.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class KakaoBookSearchClientContextTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(RestClient.Builder.class, RestClient::builder)
          .withPropertyValues(
              "external.kakao.rest-api-key=test-rest-api-key",
              "external.kakao.book-search-uri=https://dapi.kakao.com/v3/search/book")
          .withUserConfiguration(KakaoBookSearchClient.class);

  @Test
  void createsKakaoBookSearchClientWithProductionConstructor() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(KakaoBookSearchClient.class);
        });
  }
}
