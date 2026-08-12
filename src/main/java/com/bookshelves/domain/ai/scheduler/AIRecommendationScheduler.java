package com.bookshelves.domain.ai.scheduler;

import com.bookshelves.domain.ai.service.AIRecommendationService;
import com.bookshelves.global.util.ServiceTime;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 추천 도서를 전날 준비하고, 누락된 오늘 추천은 애플리케이션 시작 시 보충한다.
// LLM 호출로 진입 스레드가 지연되지 않도록 전용 실행기를 사용한다.
@Slf4j
@Component
public class AIRecommendationScheduler {

  private final AIRecommendationService aiRecommendationService;
  private final TaskExecutor taskExecutor;

  public AIRecommendationScheduler(
      AIRecommendationService aiRecommendationService,
      @Qualifier("aiRecommendationTaskExecutor") TaskExecutor taskExecutor) {
    this.aiRecommendationService = aiRecommendationService;
    this.taskExecutor = taskExecutor;
  }

  @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
  public void prepareTomorrow() {
    submit(ServiceTime.today().plusDays(1));
  }

  @EventListener(ApplicationReadyEvent.class)
  public void prepareTodayOnStartup() {
    submit(ServiceTime.today());
  }

  private void submit(LocalDate recommendedDate) {
    taskExecutor.execute(
        () -> {
          try {
            aiRecommendationService.prepare(recommendedDate);
          } catch (Exception e) {
            log.error("오늘의 추천 도서 준비 실패: recommendedDate={}", recommendedDate, e);
          }
        });
  }
}
