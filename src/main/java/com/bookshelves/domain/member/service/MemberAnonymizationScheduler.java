package com.bookshelves.domain.member.service;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MemberAnonymizationScheduler {

  private final MemberRepository memberRepository;

  public MemberAnonymizationScheduler(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void anonymizeExpiredWithdrawnMembers() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(Member.RESTORE_PERIOD_DAYS);
    List<Member> targets =
        memberRepository.findByStatusAndDeletedAtLessThanEqual(MemberStatus.WITHDRAWN, threshold);

    targets.forEach(Member::anonymize);
  }
}
