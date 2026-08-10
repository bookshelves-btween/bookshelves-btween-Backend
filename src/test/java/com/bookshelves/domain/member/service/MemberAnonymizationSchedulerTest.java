package com.bookshelves.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class MemberAnonymizationSchedulerTest {

  // MemberAnonymizationScheduler.BATCH_SIZE와 반드시 같은 값이어야 한다.
  private static final int BATCH_SIZE = 500;

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final MemberCommandService memberCommandService = mock(MemberCommandService.class);
  private final MemberAnonymizationScheduler scheduler =
      new MemberAnonymizationScheduler(memberRepository, memberCommandService);

  @Test
  void anonymizesEachMemberReturnedByRepository() {
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(List.of(1L, 2L));

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberCommandService).anonymizeMember(1L);
    verify(memberCommandService).anonymizeMember(2L);
  }

  @Test
  void continuesProcessingRemainingMembersWhenOneAnonymizationFails() {
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(List.of(1L, 2L, 3L));
    doThrow(new RuntimeException("처리 실패")).when(memberCommandService).anonymizeMember(2L);

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberCommandService).anonymizeMember(1L);
    verify(memberCommandService).anonymizeMember(2L);
    verify(memberCommandService).anonymizeMember(3L);
  }

  @Test
  void queriesWithThresholdThirtyDaysBeforeNow() {
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(List.of());

    scheduler.anonymizeExpiredWithdrawnMembers();

    ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(memberRepository)
        .findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), thresholdCaptor.capture(), any(Pageable.class));

    LocalDateTime expected =
        LocalDateTime.now(Member.SERVICE_ZONE).minusDays(Member.RESTORE_PERIOD_DAYS);
    long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(thresholdCaptor.getValue(), expected));
    assertThat(diffSeconds).isLessThan(5);
  }

  @Test
  void doesNothingWhenNoMembersAreEligible() {
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(List.of());

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberRepository)
        .findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class));
  }

  @Test
  void keepsFetchingPageZeroUntilLastPageIsShorterThanBatchSize() {
    // 첫 조회가 BATCH_SIZE만큼 꽉 차서 오면 다음 페이지가 더 있다고 보고 다시 조회해야 한다.
    // 처리된 회원은 WITHDRAWN에서 벗어나 다음 조회 결과에서 자연히 빠지므로, 페이지 번호를
    // 올리지 않고 항상 0페이지로 다시 조회하는지까지 검증한다.
    List<Long> fullPage = LongStream.rangeClosed(1, BATCH_SIZE).boxed().toList();
    List<Long> lastPage = List.of(1001L, 1002L);
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(fullPage)
        .thenReturn(lastPage);

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberCommandService, times(BATCH_SIZE + lastPage.size())).anonymizeMember(anyLong());
    verify(memberCommandService).anonymizeMember(1001L);
    verify(memberCommandService).anonymizeMember(1002L);
    verify(memberRepository, times(2))
        .findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), eq(PageRequest.of(0, BATCH_SIZE)));
  }

  @Test
  void stopsWithoutInfiniteLoopWhenFullBatchCompletelyFails() {
    // BATCH_SIZE만큼 꽉 찬 배치인데 전부 실패하면 대상 회원 상태가 안 바뀌어서, 다음 조회도
    // 매번 똑같은 결과를 돌려준다. size == BATCH_SIZE 조건만 보고 반복하면 무한 루프에
    // 빠지므로, 한 건도 성공 못 하면 그 자리에서 실행을 종료해야 한다.
    List<Long> fullFailingPage = LongStream.rangeClosed(1, BATCH_SIZE).boxed().toList();
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(fullFailingPage);
    doThrow(new RuntimeException("처리 실패")).when(memberCommandService).anonymizeMember(anyLong());

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberCommandService, times(BATCH_SIZE)).anonymizeMember(anyLong());
    verify(memberRepository, times(1))
        .findIdsByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any(), any());
  }

  @Test
  void continuesToNextBatchWhenAtLeastOneMemberSucceedsInAFullBatch() {
    // 꽉 찬 배치 안에서 단 한 명이라도 성공하면(=대상 집합이 줄어들면), 무한 루프 방지 로직이
    // 잘못 발동해서 조기 종료하면 안 된다 — 정상적으로 다음 페이지를 계속 조회해야 한다.
    List<Long> fullPageWithOneSuccess = LongStream.rangeClosed(1, BATCH_SIZE).boxed().toList();
    List<Long> lastPage = List.of();
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(fullPageWithOneSuccess)
        .thenReturn(lastPage);
    doThrow(new RuntimeException("처리 실패"))
        .when(memberCommandService)
        .anonymizeMember(org.mockito.ArgumentMatchers.longThat(id -> id != 1L));

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberRepository, times(2))
        .findIdsByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any(), any());
  }

  @Test
  void stopsAfterSinglePageWhenResultIsShorterThanBatchSize() {
    when(memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), any(), any(Pageable.class)))
        .thenReturn(List.of(1L, 2L));

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberRepository, times(1))
        .findIdsByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any(), any());
  }
}
