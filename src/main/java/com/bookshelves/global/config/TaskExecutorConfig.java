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

  // FCM 네트워크 지연이 모임 시작·취소 트랜잭션을 호출한 스케줄러를 붙잡지 않게 한다.
  @Bean
  public ThreadPoolTaskExecutor notificationPushTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 한 모임의 참여자 알림이 앞선 회원의 FCM 응답을 기다리지 않도록 동시에 전송한다.
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("notification-push-");
    executor.initialize();
    return executor;
  }

  // 추천 도서 준비 전용. 여기만 스레드가 하나다.
  //
  // 호출자를 떼어놓는 것이 1차 목적이다. 기동 훅은 메인 스레드에서, 23시 잡은 풀 크기 2짜리 공용
  // taskScheduler에서 불리는데, 둘 다 LLM 응답을 기다리게 두면 기동 지연과 모임 시작·종료 배치
  // 지연으로 번진다.
  //
  // 스레드를 하나로 묶는 것은 준비 작업끼리 겹치지 않게 하려는 것이다. 23시 직후에 배포가 겹치면
  // 내일치와 오늘치가 동시에 돌 수 있는데, 그러면 둘 다 저장 전 상태를 읽어 같은 책을 이틀 연속으로
  // 뽑는다. 날짜가 달라 unique 제약도 걸리지 않는다. 직렬화하면 뒤에 도는 쪽이 앞의 결과를 본다.
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

  // 질문 생성과 요약 생성의 크기가 같은 것은 우연이 아니라 같은 성격의 작업이기 때문이다.
  // 둘 다 모임 단위로 LLM 호출 하나를 기다리는 일이라 동시 처리량보다 큐가 넘치지 않는 쪽이 중요하다.
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
