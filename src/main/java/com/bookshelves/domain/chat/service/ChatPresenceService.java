package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.ai.service.AIQuestionGenerationService;
import com.bookshelves.domain.ai.service.QuestionVoteStore;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.dto.ChatParticipantPayload;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

// 채팅방별 접속자(presence) 추적 — 단일 서버(SimpleBroker) 전제의 인메모리 저장.
// 서버 다중화(Redis Pub/Sub 전환) 시 이 저장소도 외부화 대상.
// 접속자 수는 세션이 아니라 "회원" 단위로 센다 (한 회원이 다중 탭·재연결로 세션을
// 여러 개 가져도 1명). 회원의 첫 구독에만 JOINED, 마지막 구독 해제에만 LEFT를 broadcast한다.
@Service
@RequiredArgsConstructor
public class ChatPresenceService {

  private static final String EVENT_JOINED = "JOINED";
  private static final String EVENT_LEFT = "LEFT";

  private final MemberRepository memberRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final QuestionVoteStore questionVoteStore;
  private final AIQuestionGenerationService aiQuestionGenerationService;

  private record Subscription(Long chatroomId, Long memberId) {}

  // sessionId → (subscriptionId → 구독 정보)
  private final Map<String, Map<String, Subscription>> subscriptionsBySession = new HashMap<>();
  // chatroomId → (memberId → 해당 회원의 활성 구독 수)
  private final Map<Long, Map<Long, Integer>> memberSubscriptionsByChatroom = new HashMap<>();

  public synchronized void join(
      Long chatroomId, Long memberId, String sessionId, String subscriptionId) {
    subscriptionsBySession
        .computeIfAbsent(sessionId, k -> new HashMap<>())
        .put(subscriptionId, new Subscription(chatroomId, memberId));

    int subscriptionCount =
        memberSubscriptionsByChatroom
            .computeIfAbsent(chatroomId, k -> new HashMap<>())
            .merge(memberId, 1, Integer::sum);

    if (subscriptionCount == 1) {
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
    Map<Long, Integer> members = memberSubscriptionsByChatroom.get(chatroomId);
    return members == null ? 0 : members.size();
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

    Integer remaining =
        members.computeIfPresent(
            subscription.memberId(), (k, count) -> count > 1 ? count - 1 : null);

    if (remaining == null) {
      if (members.isEmpty()) {
        memberSubscriptionsByChatroom.remove(subscription.chatroomId());
      }
      broadcastParticipant(subscription.chatroomId(), subscription.memberId(), EVENT_LEFT);
      reevaluateQuorum(subscription.chatroomId());
    }
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
