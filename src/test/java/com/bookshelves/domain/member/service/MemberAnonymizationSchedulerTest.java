package com.bookshelves.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MemberAnonymizationSchedulerTest {

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final MemberCommandService memberCommandService = mock(MemberCommandService.class);
  private final MemberAnonymizationScheduler scheduler =
      new MemberAnonymizationScheduler(memberRepository, memberCommandService);

  @Test
  void anonymizesEachMemberReturnedByRepository() {
    Member member1 = withId(Member.createSocialMember(Provider.KAKAO, "kakao-id-1"), 1L);
    Member member2 = withId(Member.createSocialMember(Provider.KAKAO, "kakao-id-2"), 2L);
    when(memberRepository.findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any()))
        .thenReturn(List.of(member1, member2));

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberCommandService).anonymizeMember(1L);
    verify(memberCommandService).anonymizeMember(2L);
  }

  @Test
  void continuesProcessingRemainingMembersWhenOneAnonymizationFails() {
    Member member1 = withId(Member.createSocialMember(Provider.KAKAO, "kakao-id-1"), 1L);
    Member member2 = withId(Member.createSocialMember(Provider.KAKAO, "kakao-id-2"), 2L);
    Member member3 = withId(Member.createSocialMember(Provider.KAKAO, "kakao-id-3"), 3L);
    when(memberRepository.findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any()))
        .thenReturn(List.of(member1, member2, member3));
    doThrow(new RuntimeException("처리 실패")).when(memberCommandService).anonymizeMember(2L);

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberCommandService).anonymizeMember(1L);
    verify(memberCommandService).anonymizeMember(2L);
    verify(memberCommandService).anonymizeMember(3L);
  }

  @Test
  void queriesWithThresholdThirtyDaysBeforeNow() {
    when(memberRepository.findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any()))
        .thenReturn(List.of());

    scheduler.anonymizeExpiredWithdrawnMembers();

    ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(memberRepository)
        .findByStatusAndDeletedAtLessThanEqual(
            eq(MemberStatus.WITHDRAWN), thresholdCaptor.capture());

    LocalDateTime expected =
        LocalDateTime.now(Member.SERVICE_ZONE).minusDays(Member.RESTORE_PERIOD_DAYS);
    long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(thresholdCaptor.getValue(), expected));
    assertThat(diffSeconds).isLessThan(5);
  }

  @Test
  void doesNothingWhenNoMembersAreEligible() {
    when(memberRepository.findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any()))
        .thenReturn(List.of());

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberRepository)
        .findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any());
  }

  private static Member withId(Member member, Long id) {
    ReflectionTestUtils.setField(member, "id", id);
    return member;
  }
}
