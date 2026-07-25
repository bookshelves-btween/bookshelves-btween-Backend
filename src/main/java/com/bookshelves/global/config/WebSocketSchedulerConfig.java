package com.bookshelves.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// WebSocket 전용 TaskScheduler.
// STOMP SimpleBroker의 heart-beat 발신과 presence LEFT 유예 타이머에 함께 쓴다.
// @EnableScheduling이 등록하는 기본 taskScheduler와 구분하기 위해 전용 빈으로 둔다.
@Configuration
public class WebSocketSchedulerConfig {

  @Bean
  public ThreadPoolTaskScheduler webSocketTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("ws-scheduler-");
    scheduler.initialize();
    return scheduler;
  }
}
