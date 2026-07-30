package com.bookshelves.domain.ai.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// 용도별 GeminiClient를 만든다.
//
// 타임아웃이 RestClient 인스턴스에 고정되는 값이라 빈 하나를 공유할 수 없어 팩토리를 둔다.
// 용도마다 허용 지연이 다르다. 질문 생성은 모임 준비를 붙잡고 있어 짧게 끊어야 하고, 요약은 대화
// 전체가 입력이라 응답이 길며 종료 후 비동기라 여유가 있다.
//
// 두 타임아웃을 모두 호출부가 정하게 둔다. 접속 타임아웃까지 여기서 하나로 묶었더니 질문 생성이
// 5초에서 10초로 늘어나 폴백이 그만큼 늦어졌다. 공통화가 조용히 동작을 바꾼 자리다.
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
