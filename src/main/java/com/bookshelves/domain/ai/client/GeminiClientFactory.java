package com.bookshelves.domain.ai.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// 용도별 GeminiClient를 만든다.
//
// 읽기 타임아웃만 용도마다 다르다. 질문 생성은 모임 준비 중이라 오래 붙잡으면 안 되고, 요약은 대화
// 전체가 입력이라 응답이 길며 종료 후 비동기라 여유가 있다. 타임아웃이 RestClient 인스턴스에 고정되는
// 값이라 빈 하나를 공유할 수 없어 팩토리를 둔다.
//
// 접속 타임아웃은 용도와 무관하게 같은 호스트를 부르는 일이라 나누지 않는다.
@Component
public class GeminiClientFactory {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

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

  public GeminiClient create(Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
    requestFactory.setReadTimeout(readTimeout);

    // 주입받은 빌더를 그대로 변형하지 않는다. 이 팩토리는 빌더 하나를 들고 용도별로 여러 번 불리는데,
    // 원본을 건드리면 나중에 만드는 클라이언트가 앞서 설정한 타임아웃을 물려받을 수 있다.
    RestClient restClient =
        restClientBuilder
            .clone()
            .baseUrl(GeminiClient.BASE_URL)
            .requestFactory(requestFactory)
            .build();
    return new GeminiClient(restClient, objectMapper, apiKey, model);
  }
}
