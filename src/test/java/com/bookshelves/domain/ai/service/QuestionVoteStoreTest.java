package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.domain.ai.service.QuestionVoteStore.VoteRound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionVoteStoreTest {

  private static final Long CHATROOM_ID = 100L;

  private QuestionVoteStore questionVoteStore;

  @BeforeEach
  void setUp() {
    questionVoteStore = new QuestionVoteStore();
  }

  @Test
  void countsOneVotePerMember() {
    assertThat(questionVoteStore.addVote(CHATROOM_ID, 1L)).isTrue();
    assertThat(questionVoteStore.addVote(CHATROOM_ID, 1L)).isFalse();
    assertThat(questionVoteStore.addVote(CHATROOM_ID, 2L)).isTrue();

    assertThat(questionVoteStore.countVotes(CHATROOM_ID)).isEqualTo(2);
    assertThat(questionVoteStore.hasVoted(CHATROOM_ID, 1L)).isTrue();
    assertThat(questionVoteStore.hasVoted(CHATROOM_ID, 3L)).isFalse();
  }

  @Test
  void consumesRoundOnlyOnceAndAdvancesGeneration() {
    questionVoteStore.addVote(CHATROOM_ID, 1L);
    VoteRound judged = questionVoteStore.snapshot(CHATROOM_ID);

    assertThat(questionVoteStore.consumeRound(CHATROOM_ID, judged.round())).isTrue();
    // 같은 판정으로 두 번 공개하면 질문 하나가 통째로 건너뛰어진다
    assertThat(questionVoteStore.consumeRound(CHATROOM_ID, judged.round())).isFalse();
    assertThat(questionVoteStore.countVotes(CHATROOM_ID)).isZero();
    assertThat(questionVoteStore.snapshot(CHATROOM_ID).round()).isEqualTo(judged.round() + 1);
  }

  @Test
  void staleJudgementCannotConsumeVotesOfTheNextRound() {
    questionVoteStore.addVote(CHATROOM_ID, 1L);
    VoteRound staleJudgement = questionVoteStore.snapshot(CHATROOM_ID);

    // 다른 경로가 먼저 이 라운드를 닫고, 그 뒤 새 라운드에 표가 들어온 상황
    questionVoteStore.consumeRound(CHATROOM_ID, staleJudgement.round());
    questionVoteStore.addVote(CHATROOM_ID, 2L);

    assertThat(questionVoteStore.consumeRound(CHATROOM_ID, staleJudgement.round())).isFalse();
    // 새 라운드의 표는 그대로 남아 정상적인 다음 판정에 쓰인다
    assertThat(questionVoteStore.countVotes(CHATROOM_ID)).isEqualTo(1);
  }

  @Test
  void invalidateRoundDropsVotesAndInvalidatesPendingJudgement() {
    questionVoteStore.addVote(CHATROOM_ID, 1L);
    VoteRound judgedBeforeEmpty = questionVoteStore.snapshot(CHATROOM_ID);

    // 방이 비어 라운드가 무효화되고, 이후 재접속한 사람이 새로 투표한 상황
    questionVoteStore.invalidateRound(CHATROOM_ID);
    questionVoteStore.addVote(CHATROOM_ID, 2L);

    // 표만 지우고 세대를 그대로 두면 이 판정이 재접속 이후의 표를 소비해 질문을 공개해 버린다
    assertThat(questionVoteStore.consumeRound(CHATROOM_ID, judgedBeforeEmpty.round())).isFalse();
    assertThat(questionVoteStore.countVotes(CHATROOM_ID)).isEqualTo(1);
  }

  @Test
  void snapshotReadsVotesAndRoundTogether() {
    questionVoteStore.addVote(CHATROOM_ID, 1L);
    questionVoteStore.addVote(CHATROOM_ID, 2L);

    assertThat(questionVoteStore.snapshot(CHATROOM_ID)).isEqualTo(new VoteRound(0, 2));
  }

  @Test
  void consumeRoundFailsWhenNobodyVoted() {
    assertThat(questionVoteStore.consumeRound(CHATROOM_ID, 0)).isFalse();
  }
}
