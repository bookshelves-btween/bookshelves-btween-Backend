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

// 오늘의 추천 도서를 미리 만들어 둔다.
//
// 자정이 아니라 전날 23시에 도는 이유는 멘트 생성이 LLM 호출이라 시간이 걸리기 때문이다. 자정에
// 시작하면 날짜가 바뀐 직후 몇십 초 동안 홈에 추천이 비어 보인다.
//
// 준비 작업은 전용 실행기로 넘긴다. 두 진입점 모두 남을 기다리게 하면 안 되는 스레드에서 불린다.
// 기동 훅은 메인 스레드이고, 23시 잡은 모임 시작·종료 배치와 풀을 나눠 쓰는 taskScheduler다.
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

  // 23시에 서버가 내려가 있었으면 오늘 행이 비어 있다. 뜰 때 오늘치를 확인해 채운다.
  // 이미 있으면 조회 한 번으로 끝나므로 배포마다 도는 비용이 사실상 없다.
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
            // 추천 준비 실패가 다음 스케줄을 막지 않게 한다. 홈은 가장 최근 추천으로 내려간다.
            log.error("오늘의 추천 도서 준비 실패: recommendedDate={}", recommendedDate, e);
          }
        });
  }
}
