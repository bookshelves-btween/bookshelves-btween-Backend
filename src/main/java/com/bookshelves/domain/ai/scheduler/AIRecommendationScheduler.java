package com.bookshelves.domain.ai.scheduler;

import com.bookshelves.domain.ai.service.AIRecommendationService;
import com.bookshelves.global.util.ServiceTime;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 오늘의 추천 도서를 미리 만들어 둔다.
//
// 자정이 아니라 전날 23시에 도는 이유는 멘트 생성이 LLM 호출이라 시간이 걸리기 때문이다. 자정에
// 시작하면 날짜가 바뀐 직후 몇십 초 동안 홈에 추천이 비어 보인다.
@Slf4j
@Component
@RequiredArgsConstructor
public class AIRecommendationScheduler {

  private final AIRecommendationService aiRecommendationService;

  @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
  public void prepareTomorrow() {
    prepare(ServiceTime.today().plusDays(1));
  }

  // 23시에 서버가 내려가 있었으면 오늘 행이 비어 있다. 배포 직후도 마찬가지다.
  // 뜰 때 오늘치를 확인해 채우면 그 두 경우가 같은 경로로 해결된다. 이미 있으면 조회 한 번으로 끝난다.
  @EventListener(ApplicationReadyEvent.class)
  public void prepareTodayOnStartup() {
    prepare(ServiceTime.today());
  }

  private void prepare(LocalDate recommendedDate) {
    try {
      aiRecommendationService.prepare(recommendedDate);
    } catch (Exception e) {
      // 추천 준비 실패가 애플리케이션 기동이나 다음 스케줄을 막지 않게 한다.
      log.error("오늘의 추천 도서 준비 실패: recommendedDate={}", recommendedDate, e);
    }
  }
}
