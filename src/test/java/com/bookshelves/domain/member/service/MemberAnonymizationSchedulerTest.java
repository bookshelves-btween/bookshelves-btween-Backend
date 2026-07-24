package com.bookshelves.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

class MemberAnonymizationSchedulerTest {

  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final MemberAnonymizationScheduler scheduler =
      new MemberAnonymizationScheduler(memberRepository);

  @Test
  void anonymizesMembersReturnedByRepository() {
    Member member = Member.createSocialMember(Provider.KAKAO, "kakao-id");
    member.withdraw();
    when(memberRepository.findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any()))
        .thenReturn(List.of(member));

    scheduler.anonymizeExpiredWithdrawnMembers();

    assertThat(member.getStatus()).isEqualTo(MemberStatus.ANONYMIZED);
    assertThat(member.getNickname()).startsWith("탈퇴한 사용자");
    assertThat(member.getNicknameNoun()).isNull();
    assertThat(member.getNicknameModifier()).isNull();
    assertThat(member.getNicknameAnimal()).isNull();
    assertThat(member.getProvider()).isNull();
    assertThat(member.getProviderId()).isNull();
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
    assertThat(ChronoUnit.SECONDS.between(thresholdCaptor.getValue(), expected)).isLessThan(5);
  }

  @Test
  void doesNothingWhenNoMembersAreEligible() {
    when(memberRepository.findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any()))
        .thenReturn(List.of());

    scheduler.anonymizeExpiredWithdrawnMembers();

    verify(memberRepository)
        .findByStatusAndDeletedAtLessThanEqual(eq(MemberStatus.WITHDRAWN), any());
  }
}
