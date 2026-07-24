package com.bookshelves.domain.ai.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

// 질문 생성 투표 이력 — 명세에 따라 인메모리(질문 라운드 단위, 1인 1표, DB 저장 없음).
// 단일 서버(SimpleBroker) 전제이며 서버 다중화 시 presence와 함께 외부화 대상.
// 질문 생성이 시작되면 라운드가 닫힌 것으로 보고 새 투표를 원자적으로 거부한다 —
// 생성 완료 시점의 라운드 리셋(clearVotes)에 표가 휩쓸려 유실되는 것을 막기 위함.
@Component
public class QuestionVoteStore {

  public enum VoteAddResult {
    ADDED,
    DUPLICATE,
    GENERATING
  }

  // chatroomId → 이번 라운드에 투표한 회원 id
  private final Map<Long, Set<Long>> votesByChatroom = new HashMap<>();
  // 질문 생성(LLM 비동기)이 진행 중인 chatroomId — 중복 생성 방지 + 라운드 잠금
  private final Set<Long> generatingChatrooms = new HashSet<>();

  public synchronized VoteAddResult addVote(Long chatroomId, Long memberId) {
    if (generatingChatrooms.contains(chatroomId)) {
      return VoteAddResult.GENERATING;
    }
    boolean added = votesByChatroom.computeIfAbsent(chatroomId, k -> new HashSet<>()).add(memberId);
    return added ? VoteAddResult.ADDED : VoteAddResult.DUPLICATE;
  }

  public synchronized boolean hasVoted(Long chatroomId, Long memberId) {
    Set<Long> votes = votesByChatroom.get(chatroomId);
    return votes != null && votes.contains(memberId);
  }

  public synchronized int countVotes(Long chatroomId) {
    Set<Long> votes = votesByChatroom.get(chatroomId);
    return votes == null ? 0 : votes.size();
  }

  public synchronized void clearVotes(Long chatroomId) {
    votesByChatroom.remove(chatroomId);
  }

  /** 질문 생성 시작을 선점한다. 이미 생성 중이면 false. */
  public synchronized boolean tryBeginGeneration(Long chatroomId) {
    return generatingChatrooms.add(chatroomId);
  }

  public synchronized void endGeneration(Long chatroomId) {
    generatingChatrooms.remove(chatroomId);
  }
}
