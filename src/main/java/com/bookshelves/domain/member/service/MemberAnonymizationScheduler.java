package com.bookshelves.domain.member.service;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemberAnonymizationScheduler {

  private final MemberRepository memberRepository;
  private final MemberCommandService memberCommandService;

  public MemberAnonymizationScheduler(
      MemberRepository memberRepository, MemberCommandService memberCommandService) {
    this.memberRepository = memberRepository;
    this.memberCommandService = memberCommandService;
  }

  @Scheduled(cron = "0 0 0 * * *")
  public void anonymizeExpiredWithdrawnMembers() {
    LocalDateTime threshold =
        LocalDateTime.now(Member.SERVICE_ZONE).minusDays(Member.RESTORE_PERIOD_DAYS);
    List<Member> targets =
        memberRepository.findByStatusAndDeletedAtLessThanEqual(MemberStatus.WITHDRAWN, threshold);

    for (Member member : targets) {
      // 회원 단위로 격리 — 한 건 실패가 나머지 익명화 처리를 막지 않도록 한다
      try {
        memberCommandService.anonymizeMember(member.getId());
      } catch (Exception e) {
        log.error("회원 익명화 처리 실패: memberId={}", member.getId(), e);
      }
    }
  }
}
