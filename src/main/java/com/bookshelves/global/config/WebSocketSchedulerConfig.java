package com.bookshelves.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 기본 스케줄러와 WebSocket·모임 시작 작업의 스케줄러를 분리한다.
@Configuration
public class WebSocketSchedulerConfig {

  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("app-scheduler-");
    scheduler.initialize();
    return scheduler;
  }

  @Bean
  public ThreadPoolTaskScheduler webSocketTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("ws-scheduler-");
    scheduler.initialize();
    return scheduler;
  }

  @Bean
  public ThreadPoolTaskScheduler meetingStartTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(4);
    scheduler.setThreadNamePrefix("meeting-start-");
    scheduler.initialize();
    return scheduler;
  }
}
