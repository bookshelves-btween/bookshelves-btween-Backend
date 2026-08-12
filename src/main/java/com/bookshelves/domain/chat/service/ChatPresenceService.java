package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.ai.service.QuestionRevealService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

// 채팅방 접속자를 회원 단위로 추적하는 단일 서버용 인메모리 저장소.
// 첫 구독과 마지막 해제에만 상태를 전파하며, 짧은 재접속에는 LEFT 유예를 적용한다.
// 서버 다중화 시 투표 저장소와 함께 외부화해야 한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

  private static final String EVENT_JOINED = "JOINED";
  private static final String EVENT_LEFT = "LEFT";
  // 유예 중인 회원도 접속 인원에 포함한다.
  private static final Duration LEFT_GRACE = Duration.ofSeconds(15);

  private final MemberRepository memberRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final QuestionVoteStore questionVoteStore;
  private final QuestionRevealService questionRevealService;
  // WebSocket 전용 스케줄러를 다른 TaskScheduler와 구분해 주입한다.
  private final ThreadPoolTaskScheduler webSocketTaskScheduler;

  private record Subscription(Long chatroomId, Long memberId) {}

  // sessionId → subscriptionId → 구독 정보
  private final Map<String, Map<String, Subscription>> subscriptionsBySession = new HashMap<>();
  // chatroomId → memberId → 활성 구독 수
  private final Map<Long, Map<Long, Integer>> memberSubscriptionsByChatroom = new HashMap<>();
  // chatroomId → memberId → LEFT 유예 타이머
  private final Map<Long, Map<Long, ScheduledFuture<?>>> pendingLeaveByChatroom = new HashMap<>();

  // disconnect와의 경합을 막기 위해 DB 조회 전에 구독을 등록한다.
  // 닉네임은 전역 모니터 밖에서 조회해 다른 채팅방의 presence 처리를 막지 않는다.
  public void join(Long chatroomId, Long memberId, String sessionId, String subscriptionId) {
    if (!register(chatroomId, memberId, sessionId, subscriptionId)) {
      return;
    }

    String nickname = findNickname(memberId);

    // 카운트 계산과 전송을 직렬화해 접속자 수 프레임의 역행을 막는다.
    synchronized (this) {
      broadcastParticipant(chatroomId, nickname, EVENT_JOINED);
    }
  }

  /** 구독을 등록하고 JOINED를 전파해야 하는지 알려준다. */
  private synchronized boolean register(
      Long chatroomId, Long memberId, String sessionId, String subscriptionId) {
    Map<String, Subscription> sessionSubscriptions =
        subscriptionsBySession.computeIfAbsent(sessionId, k -> new HashMap<>());

    // 중복 SUBSCRIBE는 활성 구독 수를 늘리지 않는다.
    if (sessionSubscriptions.containsKey(subscriptionId)) {
      return false;
    }
    sessionSubscriptions.put(subscriptionId, new Subscription(chatroomId, memberId));

    boolean wasPendingLeave = cancelPendingLeave(chatroomId, memberId);

    int subscriptionCount =
        memberSubscriptionsByChatroom
            .computeIfAbsent(chatroomId, k -> new HashMap<>())
            .merge(memberId, 1, Integer::sum);

    // 유예 중인 회원은 이미 접속자로 집계되므로 JOINED를 다시 보내지 않는다.
    return subscriptionCount == 1 && !wasPendingLeave;
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

  // 정족수는 접속 인원의 절반을 올림한 값이다.
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
      return;
    }

    if (current > 1) {
      members.put(subscription.memberId(), current - 1);
      return;
    }

    // 마지막 구독 해제 후 유예 시간 동안 재접속을 기다린다.
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

  // LEFT 확정과 정족수 재판정은 원자적으로 처리하되, DB 락을 사용하는 질문 공개는 모니터 밖에서 실행한다.
  private void finalizeLeave(Long chatroomId, Long memberId, ScheduledFuture<?> expected) {
    // 전역 모니터를 점유하지 않도록 닉네임을 먼저 조회한다.
    String nickname = findNickname(memberId);

    Integer roundToReveal;
    synchronized (this) {
      Map<Long, ScheduledFuture<?>> pending = pendingLeaveByChatroom.get(chatroomId);
      if (pending == null || pending.get(memberId) != expected) {
        // 취소됐거나 새 타이머로 교체된 작업은 무시한다.
        return;
      }

      pending.remove(memberId);
      if (pending.isEmpty()) {
        pendingLeaveByChatroom.remove(chatroomId);
      }

      broadcastParticipant(chatroomId, nickname, EVENT_LEFT);
      roundToReveal = quorumRoundToReveal(chatroomId);
    }

    if (roundToReveal == null) {
      return;
    }
    // 질문 공개 실패는 이미 확정된 presence 상태를 되돌리지 않는다.
    try {
      questionRevealService.revealNext(chatroomId, roundToReveal);
    } catch (Exception e) {
      log.error("정족수 재판정 중 질문 공개 실패: chatroomId={}", chatroomId, e);
    }
  }

  // LEFT로 낮아진 정족수를 기존 표가 충족하면 공개할 라운드 번호를 반환한다.
  private Integer quorumRoundToReveal(Long chatroomId) {
    if (countConnected(chatroomId) == 0) {
      // 빈 방의 표와 이전 정족수 판정을 함께 무효화한다.
      questionVoteStore.invalidateRound(chatroomId);
      return null;
    }

    int requiredVotes = requiredVotes(chatroomId);
    // 투표 경로와의 경합을 막기 위해 표와 라운드를 함께 읽는다.
    QuestionVoteStore.VoteRound voteRound = questionVoteStore.snapshot(chatroomId);
    if (voteRound.votes() >= 1 && voteRound.votes() >= requiredVotes) {
      return voteRound.round();
    }
    return null;
  }

  // 닉네임 조회 실패가 presence 정리와 정족수 판정을 막지 않게 한다.
  private String findNickname(Long memberId) {
    try {
      return memberRepository.findById(memberId).map(m -> m.getNickname()).orElse(null);
    } catch (Exception e) {
      log.warn("presence 프레임 닉네임 조회에 실패해 닉네임 없이 내보낸다: memberId={}", memberId, e);
      return null;
    }
  }

  private void broadcastParticipant(Long chatroomId, String nickname, String event) {
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
