package com.bookshelves.domain.ai.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// 용도별 타임아웃이 적용된 GeminiClient를 생성한다.
@Component
public class GeminiClientFactory {

  private final RestClient.Builder restClientBuilder;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public GeminiClientFactory(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${external.gemini.api-key}") String apiKey,
      @Value("${external.gemini.model:" + GeminiClient.DEFAULT_MODEL + "}") String model) {
    this.restClientBuilder = restClientBuilder;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.model = model;
  }

  public GeminiClient create(Duration connectTimeout, Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);

    // 다음 클라이언트가 이전 타임아웃 설정을 물려받지 않도록 빌더를 복제한다.
    RestClient restClient =
        restClientBuilder
            .clone()
            .baseUrl(GeminiClient.BASE_URL)
            .requestFactory(requestFactory)
            .build();
    return new GeminiClient(restClient, objectMapper, apiKey, model);
  }
}
