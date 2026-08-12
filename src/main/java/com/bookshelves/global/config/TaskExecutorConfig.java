package com.bookshelves.global.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 네트워크 대기가 긴 비동기 작업별로 실행기와 큐를 분리한다.
@Configuration
public class TaskExecutorConfig {

  @Bean
  public ThreadPoolTaskExecutor aiQuestionTaskExecutor() {
    return buildExecutor("ai-question-");
  }

  // 회의 요약과 질문 생성의 실행기·큐를 분리해 한쪽의 지연이 전파되지 않게 한다.
  @Bean
  public ThreadPoolTaskExecutor meetingSummaryTaskExecutor() {
    return buildExecutor("meeting-summary-");
  }

  @Bean
  public ThreadPoolTaskExecutor notificationPushTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(500);
    // 큐가 가득 차면 호출자 실행 대신 별도 재시도 경로로 넘긴다.
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.setThreadNamePrefix("notification-push-");
    executor.initialize();
    return executor;
  }

  @Bean
  public ThreadPoolTaskScheduler notificationPushRetryScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("notification-push-retry-");
    scheduler.initialize();
    return scheduler;
  }

  // 추천 준비를 직렬화해 오늘치와 내일치가 같은 책을 동시에 선택하지 않게 한다.
  @Bean
  public ThreadPoolTaskExecutor aiRecommendationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("ai-recommendation-");
    executor.initialize();
    return executor;
  }

  // 모임 단위 LLM 호출에 공통으로 사용하는 실행기 설정.
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
