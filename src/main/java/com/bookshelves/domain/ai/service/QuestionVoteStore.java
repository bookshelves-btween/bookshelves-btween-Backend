package com.bookshelves.domain.ai.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

// 질문 공개 투표 이력 — 명세에 따라 인메모리(질문 라운드 단위, 1인 1표, DB 저장 없음).
// 단일 서버(SimpleBroker) 전제이며 서버 다중화 시 presence와 함께 외부화 대상.
//
// 질문은 모임 시작 전에 미리 저장되므로 라운드 중에 비동기 생성 작업이 돌지 않는다 —
// "생성 중이라 투표를 막는" 상태가 존재하지 않아 중복 여부만 판정하면 된다.
@Component
public class QuestionVoteStore {

  // chatroomId → 이번 라운드에 투표한 회원 id
  private final Map<Long, Set<Long>> votesByChatroom = new HashMap<>();
  // chatroomId → 라운드 번호. 질문이 공개될 때마다 증가하며, 낡은 판정으로 공개되는 것을 막는 세대 값이다.
  private final Map<Long, Integer> roundByChatroom = new HashMap<>();

  /** 이번 라운드에 표를 더한다. 이미 투표했으면 false. */
  public synchronized boolean addVote(Long chatroomId, Long memberId) {
    return votesByChatroom.computeIfAbsent(chatroomId, key -> new HashSet<>()).add(memberId);
  }

  public synchronized boolean hasVoted(Long chatroomId, Long memberId) {
    Set<Long> votes = votesByChatroom.get(chatroomId);
    return votes != null && votes.contains(memberId);
  }

  public synchronized int countVotes(Long chatroomId) {
    Set<Long> votes = votesByChatroom.get(chatroomId);
    return votes == null ? 0 : votes.size();
  }

  /**
   * 표를 버리고 라운드를 무효화한다. 방이 비어 라운드를 이어갈 이유가 없을 때 쓴다.
   *
   * <p>표만 지우고 세대를 그대로 두면 안 된다 — 표를 지우기 전에 정족수를 판정해 둔 요청이, 이후 재접속으로 들어온 새 표를 같은 세대로 인식해 소비하고 질문을 공개해
   * 버린다.
   */
  public synchronized void invalidateRound(Long chatroomId) {
    votesByChatroom.remove(chatroomId);
    roundByChatroom.merge(chatroomId, 1, Integer::sum);
  }

  /**
   * 정족수 판정에 쓸 표 수와 라운드 번호를 한 번에 읽는다.
   *
   * <p>두 값을 따로 읽으면 그 사이에 다른 경로가 라운드를 닫을 수 있다. 그러면 "이전 라운드의 표 수"와 "새 라운드의 세대"를 짝지어 판정하게 되어, 낡은 판정이 새
   * 라운드의 표를 소비한다.
   */
  public synchronized VoteRound snapshot(Long chatroomId) {
    return new VoteRound(roundByChatroom.getOrDefault(chatroomId, 0), countVotes(chatroomId));
  }

  /** 정족수를 판정한 시점의 표 수와 라운드 번호 — 반드시 같은 스냅샷에서 나와야 한다. */
  public record VoteRound(int round, int votes) {}

  /**
   * {@code expectedRound} 라운드의 표를 원자적으로 가져가며 라운드를 닫는다. 이미 닫힌 라운드이거나 가져갈 표가 없으면 false.
   *
   * <p>질문 공개를 요청하는 경로가 둘이고(투표 정족수 도달, 접속자 이탈로 인한 정족수 재판정) 서로 다른 락 아래에서 각자 정족수를 판정하므로, 같은 표를 근거로 두
   * 경로가 각각 커서를 올려 질문 하나를 통째로 건너뛸 수 있다.
   *
   * <p>"표가 남아 있는지"만 보면 부족하다 — 앞선 요청이 모임 행 락을 쥔 사이 다음 라운드의 표가 들어오면, 뒤늦게 락을 얻은 요청이 그 새 표를 먹고 정족수와
   * 무관하게 커서를 또 올린다. 그래서 표가 아니라 라운드 번호로 판정한다.
   */
  public synchronized boolean consumeRound(Long chatroomId, int expectedRound) {
    if (roundByChatroom.getOrDefault(chatroomId, 0) != expectedRound) {
      return false; // 판정 이후 다른 경로가 이미 이 라운드를 닫았다
    }
    roundByChatroom.put(chatroomId, expectedRound + 1);
    Set<Long> votes = votesByChatroom.remove(chatroomId);
    return votes != null && !votes.isEmpty();
  }
}
