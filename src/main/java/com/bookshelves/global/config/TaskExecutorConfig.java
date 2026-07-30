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
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("ai-question-");
    executor.initialize();
    return executor;
  }

  // 요약 생성 전용. 질문 생성과 큐를 나눠 한쪽이 밀려도 다른 쪽이 막히지 않게 한다.
  // 요약은 대화 전체를 입력으로 넣어 호출이 길다.
  @Bean
  public ThreadPoolTaskExecutor meetingSummaryTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("meeting-summary-");
    executor.initialize();
    return executor;
  }
}
