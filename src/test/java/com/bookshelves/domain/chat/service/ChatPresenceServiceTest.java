package com.bookshelves.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.ai.service.QuestionRevealService;
import com.bookshelves.domain.ai.service.QuestionVoteStore;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 닉네임 조회는 presence 판정보다 먼저, 락 밖에서 실행된다. 그 조회가 실패해도 presence 상태 기계는
// 끝까지 돌아야 한다 — 특히 LEFT 확정은 유예 타이머가 이미 발화한 뒤라 재시도가 없다.
class ChatPresenceServiceTest {

  private static final Long CHATROOM_ID = 1L;
  private static final Long MEMBER_ID = 10L;

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
  private final QuestionVoteStore questionVoteStore = mock(QuestionVoteStore.class);
  private final QuestionRevealService questionRevealService = mock(QuestionRevealService.class);
  private final ThreadPoolTaskScheduler scheduler = mock(ThreadPoolTaskScheduler.class);

  private final ChatPresenceService presenceService =
      new ChatPresenceService(
          memberRepository, messagingTemplate, questionVoteStore, questionRevealService, scheduler);

  @Test
  void joinStillCountsMemberWhenNicknameLookupFails() {
    givenNicknameLookupFails();

    assertThatCode(() -> presenceService.join(CHATROOM_ID, MEMBER_ID, "session-1", "sub-1"))
        .doesNotThrowAnyException();

    assertThat(presenceService.countConnected(CHATROOM_ID)).isEqualTo(1);
  }

  // 등록이 닉네임 조회 뒤로 밀리면, 그 사이 소켓이 끊겨 disconnect가 먼저 도는 순간 지울 상태가
  // 없어 그냥 반환하고 뒤늦은 등록이 이미 닫힌 세션을 남긴다. 그 세션에는 다시 올 해제 이벤트가
  // 없어 회원이 영구히 접속자로 세어진다. 조회 시점에 이미 등록이 끝나 있어야 한다.
  @Test
  void subscriptionIsRegisteredBeforeNicknameLookup() {
    AtomicInteger connectedDuringLookup = new AtomicInteger(-1);
    given(memberRepository.findById(MEMBER_ID))
        .willAnswer(
            invocation -> {
              connectedDuringLookup.set(presenceService.countConnected(CHATROOM_ID));
              return Optional.empty();
            });

    presenceService.join(CHATROOM_ID, MEMBER_ID, "session-1", "sub-1");

    assertThat(connectedDuringLookup).hasValue(1);
  }

  // 중복 SUBSCRIBE는 전파할 것이 없으므로 DB까지 갈 이유가 없다
  @Test
  void duplicateSubscribeDoesNotHitTheDatabase() {
    presenceService.join(CHATROOM_ID, MEMBER_ID, "session-1", "sub-1");
    clearInvocations(memberRepository);

    presenceService.join(CHATROOM_ID, MEMBER_ID, "session-1", "sub-1");

    verifyNoInteractions(memberRepository);
  }

  // 조회 실패가 락 앞에서 터져 나가면 pending 항목이 남아 떠난 회원이 영영 접속자로 세어진다.
  // 유예 타이머는 한 번 발화하면 끝이라 스스로 복구되지 않는다.
  @Test
  void leaveIsFinalizedWhenNicknameLookupFails() {
    presenceService.join(CHATROOM_ID, MEMBER_ID, "session-1", "sub-1");
    Runnable leaveTask = captureLeaveTask();
    givenNicknameLookupFails();

    assertThatCode(leaveTask::run).doesNotThrowAnyException();

    assertThat(presenceService.countConnected(CHATROOM_ID)).isZero();
  }

  // 유예 동안에는 아직 접속자로 센다 — 타이머가 돌기 전까지는 숫자가 깜빡이지 않아야 한다
  @Test
  void memberIsStillCountedDuringLeaveGrace() {
    presenceService.join(CHATROOM_ID, MEMBER_ID, "session-1", "sub-1");
    presenceService.unsubscribe("session-1", "sub-1");

    assertThat(presenceService.countConnected(CHATROOM_ID)).isEqualTo(1);
  }

  private void givenNicknameLookupFails() {
    willThrow(new QueryTimeoutException("커넥션 획득 실패")).given(memberRepository).findById(MEMBER_ID);
  }

  // 유예 타이머의 태스크를 잡아 둔다. 즉시 실행하면 schedule이 반환한 future를 pending에 넣기 전이라
  // finalizeLeave의 세대 비교가 성립하지 않는다.
  private Runnable captureLeaveTask() {
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    given(scheduler.schedule(task.capture(), any(Instant.class))).willAnswer(invocation -> future);

    presenceService.unsubscribe("session-1", "sub-1");

    return task.getValue();
  }
}
