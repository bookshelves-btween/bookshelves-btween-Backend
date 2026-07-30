package com.bookshelves.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// AI 질문 생성(LLM 호출) 전용 실행기.
// Boot 기본 applicationTaskExecutor는 WebSocket 브로커가 채널용 executor를 등록하면
// 자동 구성이 꺼지므로, 용도를 명시한 전용 빈을 둔다.
@Configuration
public class TaskExecutorConfig {

  @Bean
  public ThreadPoolTaskExecutor aiQuestionTaskExecutor() {
    return buildExecutor("ai-question-");
  }

  // 요약 생성 전용. 질문 생성과 큐를 나눠 한쪽이 밀려도 다른 쪽이 막히지 않게 한다.
  // 요약은 대화 전체를 입력으로 넣어 호출이 길다.
  @Bean
  public ThreadPoolTaskExecutor meetingSummaryTaskExecutor() {
    return buildExecutor("meeting-summary-");
  }

  // 추천 도서 준비 전용. 하루 한 번이라 큐가 밀릴 일은 없고, 목적은 호출자를 떼어놓는 것이다.
  // 기동 훅은 메인 스레드에서, 23시 잡은 풀 크기 2짜리 공용 taskScheduler에서 불린다.
  // 둘 다 LLM 응답을 기다리게 두면 각각 기동 지연과 모임 시작·종료 배치 지연으로 번진다.
  @Bean
  public ThreadPoolTaskExecutor aiRecommendationTaskExecutor() {
    return buildExecutor("ai-recommendation-");
  }

  // 세 실행기의 크기가 같은 것은 우연이 아니라 같은 성격의 작업이기 때문이다.
  // 둘 다 LLM 호출 하나를 기다리는 일이라 동시 처리량보다 큐가 넘치지 않는 쪽이 중요하다.
  private ThreadPoolTaskExecutor buildExecutor(String threadNamePrefix) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.initialize();
    return executor;
  }
}
