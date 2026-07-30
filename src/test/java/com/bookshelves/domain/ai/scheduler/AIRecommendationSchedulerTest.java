package com.bookshelves.domain.ai.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.service.AIRecommendationService;
import com.bookshelves.global.util.ServiceTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

@ExtendWith(MockitoExtension.class)
class AIRecommendationSchedulerTest {

  @Mock private AIRecommendationService aiRecommendationService;

  private AIRecommendationScheduler scheduler;

  @BeforeEach
  void setUp() {
    // 실행기를 동기로 바꿔 제출된 작업을 그 자리에서 검사한다. 비동기 여부가 아니라
    // 무슨 날짜로 무엇을 부르는지가 이 테스트의 관심사다.
    scheduler = new AIRecommendationScheduler(aiRecommendationService, new SyncTaskExecutor());
  }

  @Test
  void nightlyRunPreparesTomorrow() {
    scheduler.prepareTomorrow();

    // 23시에 도는 잡이 만드는 것은 내일 것이다. 오늘로 만들면 하루가 통째로 밀린다.
    verify(aiRecommendationService).prepare(ServiceTime.today().plusDays(1));
  }

  @Test
  void startupPreparesToday() {
    scheduler.prepareTodayOnStartup();

    verify(aiRecommendationService).prepare(ServiceTime.today());
  }

  @Test
  void swallowsFailureSoStartupAndNextScheduleAreNotBlocked() {
    willThrow(new IllegalStateException("GEMINI_API_KEY가 설정되지 않았습니다."))
        .given(aiRecommendationService)
        .prepare(ServiceTime.today());

    assertThatCode(() -> scheduler.prepareTodayOnStartup()).doesNotThrowAnyException();
  }
}
