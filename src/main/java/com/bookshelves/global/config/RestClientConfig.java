package com.bookshelves.global.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  // 개별 클라이언트가 자체 타임아웃을 설정하지 않은 경우를 대비한 기본값이다.
  // 외부 서버가 응답을 지연하거나 주지 않을 때 요청이 무한정 대기하는 것을 막는다.
  @Bean
  public RestClient.Builder restClientBuilder() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(10));

    return RestClient.builder().requestFactory(requestFactory);
  }
}
