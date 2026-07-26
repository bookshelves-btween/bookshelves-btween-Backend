package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.ai.service.AIQuestionGenerationService;
import com.bookshelves.domain.ai.service.QuestionVoteStore;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.dto.ChatParticipantPayload;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

// 채팅방별 접속자(presence) 추적 — 단일 서버(SimpleBroker) 전제의 인메모리 저장.
// 서버 다중화(Redis Pub/Sub 전환) 시 이 저장소도 외부화 대상.
// 접속자 수는 세션이 아니라 "회원" 단위로 센다 (한 회원이 다중 탭·재연결로 세션을
// 여러 개 가져도 1명). 회원의 첫 구독에만 JOINED, 마지막 구독 해제에만 LEFT를 broadcast한다.
//
// LEFT는 즉시 내보내지 않고 유예(grace)를 둔다 — 마지막 구독이 끊겨도 유예 시간 안에
// 재접속하면 LEFT 없이 presence를 유지한다. iOS 백그라운드 전환처럼 짧게 끊겼다 돌아오는
// 경우에 connected 숫자가 깜빡이는 것을 막는다. 유예 동안에도 회원을 접속자로 계속 센다.
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

  private static final String EVENT_JOINED = "JOINED";
  private static final String EVENT_LEFT = "LEFT";
  // LEFT 유예 시간 — 명세 "10~15초, 백엔드 확정" 범위에서 15초로 확정
  private static final Duration LEFT_GRACE = Duration.ofSeconds(15);

  private final MemberRepository memberRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final QuestionVoteStore questionVoteStore;
  private final AIQuestionGenerationService aiQuestionGenerationService;
  // 필드명이 빈 이름(webSocketTaskScheduler)과 일치 → 브로커 자체 스케줄러와 구분해 주입
  private final ThreadPoolTaskScheduler webSocketTaskScheduler;

  private record Subscription(Long chatroomId, Long memberId) {}

  // sessionId → (subscriptionId → 구독 정보)
  private final Map<String, Map<String, Subscription>> subscriptionsBySession = new HashMap<>();
  // chatroomId → (memberId → 해당 회원의 활성 구독 수)
  private final Map<Long, Map<Long, Integer>> memberSubscriptionsByChatroom = new HashMap<>();
  // chatroomId → (memberId → LEFT 유예 타이머). 활성 구독이 0이지만 아직 접속자로 세는 회원.
  private final Map<Long, Map<Long, ScheduledFuture<?>>> pendingLeaveByChatroom = new HashMap<>();

  public synchronized void join(
      Long chatroomId, Long memberId, String sessionId, String subscriptionId) {
    Map<String, Subscription> sessionSubscriptions =
        subscriptionsBySession.computeIfAbsent(sessionId, k -> new HashMap<>());

    // 같은 (sessionId, subscriptionId) 중복 SUBSCRIBE는 멱등 처리 — 덮어쓰면서 카운트만 올리면
    // 이후 해제로 상쇄되지 않아 phantom presence(과대 집계)가 영구히 남는다
    if (sessionSubscriptions.containsKey(subscriptionId)) {
      return;
    }
    sessionSubscriptions.put(subscriptionId, new Subscription(chatroomId, memberId));

    // 유예 중이던 회원의 재접속이면 타이머를 취소한다
    boolean wasPendingLeave = cancelPendingLeave(chatroomId, memberId);

    int subscriptionCount =
        memberSubscriptionsByChatroom
            .computeIfAbsent(chatroomId, k -> new HashMap<>())
            .merge(memberId, 1, Integer::sum);

    // 유예 중이던 회원은 이미 접속자로 세고 있었으므로 JOINED를 다시 쏘지 않는다(깜빡임 방지)
    if (subscriptionCount == 1 && !wasPendingLeave) {
      broadcastParticipant(chatroomId, memberId, EVENT_JOINED);
    }
  }

  public synchronized void unsubscribe(String sessionId, String subscriptionId) {
    Map<String, Subscription> subscriptions = subscriptionsBySession.get(sessionId);
    if (subscriptions == null) {
      return;
    }

    Subscription subscription = subscriptions.remove(subscriptionId);
    if (subscriptions.isEmpty()) {
      subscriptionsBySession.remove(sessionId);
    }
    if (subscription != null) {
      release(subscription);
    }
  }

  public synchronized void disconnect(String sessionId) {
    Map<String, Subscription> subscriptions = subscriptionsBySession.remove(sessionId);
    if (subscriptions == null) {
      return;
    }
    subscriptions.values().forEach(this::release);
  }

  public synchronized int countConnected(Long chatroomId) {
    Map<Long, Integer> active = memberSubscriptionsByChatroom.get(chatroomId);
    Map<Long, ScheduledFuture<?>> pending = pendingLeaveByChatroom.get(chatroomId);

    Set<Long> members = new HashSet<>();
    if (active != null) {
      members.addAll(active.keySet());
    }
    if (pending != null) {
      members.addAll(pending.keySet());
    }
    return members.size();
  }

  // 정족수 — 명세 정의 공식: ceil(connected / 2). (예시: 4명 → 2표, 5명 → 3표)
  public synchronized int requiredVotes(Long chatroomId) {
    return Math.ceilDiv(countConnected(chatroomId), 2);
  }

  private void release(Subscription subscription) {
    Map<Long, Integer> members = memberSubscriptionsByChatroom.get(subscription.chatroomId());
    if (members == null) {
      return;
    }

    Integer current = members.get(subscription.memberId());
    if (current == null) {
      return; // 이미 정리된 구독 — 방어
    }

    if (current > 1) {
      members.put(subscription.memberId(), current - 1);
      return;
    }

    // 회원의 마지막 활성 구독이 끊겼다 — 즉시 LEFT하지 않고 유예 타이머를 건다.
    // 유예 동안에도 접속자로 계속 세므로 connected가 깜빡이지 않는다.
    members.remove(subscription.memberId());
    if (members.isEmpty()) {
      memberSubscriptionsByChatroom.remove(subscription.chatroomId());
    }
    scheduleLeave(subscription.chatroomId(), subscription.memberId());
  }

  private void scheduleLeave(Long chatroomId, Long memberId) {
    ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
    holder[0] =
        webSocketTaskScheduler.schedule(
            () -> finalizeLeave(chatroomId, memberId, holder[0]), Instant.now().plus(LEFT_GRACE));
    pendingLeaveByChatroom
        .computeIfAbsent(chatroomId, k -> new HashMap<>())
        .put(memberId, holder[0]);
  }

  private boolean cancelPendingLeave(Long chatroomId, Long memberId) {
    Map<Long, ScheduledFuture<?>> pending = pendingLeaveByChatroom.get(chatroomId);
    if (pending == null) {
      return false;
    }

    ScheduledFuture<?> future = pending.remove(memberId);
    if (pending.isEmpty()) {
      pendingLeaveByChatroom.remove(chatroomId);
    }
    if (future == null) {
      return false;
    }
    future.cancel(false);
    return true;
  }

  // 유예 시간이 지나도 재접속이 없으면 실제 LEFT를 확정한다.
  private synchronized void finalizeLeave(
      Long chatroomId, Long memberId, ScheduledFuture<?> expected) {
    Map<Long, ScheduledFuture<?>> pending = pendingLeaveByChatroom.get(chatroomId);
    if (pending == null || pending.get(memberId) != expected) {
      // 재접속으로 취소됐거나, 해제→재접속→재해제로 새 타이머가 등록된 경우 — 이 태스크는 무시
      return;
    }

    pending.remove(memberId);
    if (pending.isEmpty()) {
      pendingLeaveByChatroom.remove(chatroomId);
    }
    broadcastParticipant(chatroomId, memberId, EVENT_LEFT);
    reevaluateQuorum(chatroomId);
  }

  // 명세 "정족수 즉시 재판정" — LEFT로 connected가 줄어 requiredVotes가 내려갔을 때,
  // 이미 모인 표가 새 정족수를 충족하면 그 자리에서 질문 생성을 시작한다.
  private void reevaluateQuorum(Long chatroomId) {
    if (countConnected(chatroomId) == 0) {
      // 방이 비면 라운드 자체가 무의미 — 남은 표를 정리하고 재판정하지 않는다
      questionVoteStore.clearVotes(chatroomId);
      return;
    }

    int requiredVotes = requiredVotes(chatroomId);
    int currentVotes = questionVoteStore.countVotes(chatroomId);
    if (currentVotes >= 1 && currentVotes >= requiredVotes) {
      aiQuestionGenerationService.requestGeneration(chatroomId, requiredVotes);
    }
  }

  private void broadcastParticipant(Long chatroomId, Long memberId, String event) {
    String nickname = memberRepository.findById(memberId).map(m -> m.getNickname()).orElse(null);

    ChatParticipantPayload payload =
        new ChatParticipantPayload(
            event,
            nickname,
            countConnected(chatroomId),
            requiredVotes(chatroomId),
            questionVoteStore.countVotes(chatroomId));

    messagingTemplate.convertAndSend(
        ChatFrame.CHATROOM_SUB_DESTINATION + chatroomId,
        ChatFrame.of(ChatFrame.TYPE_PARTICIPANT, chatroomId, payload));
  }
}
