package com.bookshelves.domain.ai.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

// 질문 라운드별 1인 1표를 관리하는 인메모리 저장소.
// 단일 서버 전제이며 서버 다중화 시 외부 저장소로 이전해야 한다.
@Component
public class QuestionVoteStore {

  // chatroomId → 현재 라운드에 투표한 회원 ID
  private final Map<Long, Set<Long>> votesByChatroom = new HashMap<>();
  // chatroomId → 현재 라운드 번호
  private final Map<Long, Integer> roundByChatroom = new HashMap<>();

  /** 현재 라운드에 투표하며, 중복 투표이면 false를 반환한다. */
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

  /** 표를 지우고 라운드 번호를 올려 이전 정족수 판정을 무효화한다. */
  public synchronized void invalidateRound(Long chatroomId) {
    votesByChatroom.remove(chatroomId);
    roundByChatroom.merge(chatroomId, 1, Integer::sum);
  }

  /** 정족수 판정에 사용할 라운드 번호와 표 수를 같은 스냅샷으로 읽는다. */
  public synchronized VoteRound snapshot(Long chatroomId) {
    return new VoteRound(roundByChatroom.getOrDefault(chatroomId, 0), countVotes(chatroomId));
  }

  /** 정족수 판정 시점의 라운드 번호와 표 수. */
  public record VoteRound(int round, int votes) {}

  /**
   * 예상한 라운드의 표를 소비하고 라운드를 닫는다. 이미 닫혔거나 표가 없으면 false를 반환한다. 라운드 번호를 함께 비교해 이전 정족수 판정이 새 라운드의 표를 소비하지
   * 못하게 한다.
   */
  public synchronized boolean consumeRound(Long chatroomId, int expectedRound) {
    if (roundByChatroom.getOrDefault(chatroomId, 0) != expectedRound) {
      return false;
    }
    roundByChatroom.put(chatroomId, expectedRound + 1);
    Set<Long> votes = votesByChatroom.remove(chatroomId);
    return votes != null && !votes.isEmpty();
  }
}
