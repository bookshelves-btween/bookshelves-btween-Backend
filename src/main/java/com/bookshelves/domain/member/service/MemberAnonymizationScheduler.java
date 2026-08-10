package com.bookshelves.domain.member.service;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemberAnonymizationScheduler {

  // 처리 대상을 이 개수만큼씩 끊어서 조회한다. 대량 탈퇴가 몰린 날이어도 한 번에 메모리에
  // 올라가는 건수를 이 값으로 상한을 둔다.
  private static final int BATCH_SIZE = 500;

  private final MemberRepository memberRepository;
  private final MemberCommandService memberCommandService;

  public MemberAnonymizationScheduler(
      MemberRepository memberRepository, MemberCommandService memberCommandService) {
    this.memberRepository = memberRepository;
    this.memberCommandService = memberCommandService;
  }

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public void anonymizeExpiredWithdrawnMembers() {
    LocalDateTime threshold =
        LocalDateTime.now(Member.SERVICE_ZONE).minusDays(Member.RESTORE_PERIOD_DAYS);

    // 처리된 회원은 상태가 WITHDRAWN에서 벗어나 다음 조회 결과에서 자연히 빠지므로, 페이지
    // 번호를 올리지 않고 항상 0페이지를 반복 조회한다. 페이지 번호를 올리면 그사이 대상
    // 집합이 줄어들어 일부 회원을 건너뛰게 된다.
    Pageable pageable = PageRequest.of(0, BATCH_SIZE);
    List<Long> memberIds;
    do {
      memberIds =
          memberRepository.findIdsByStatusAndDeletedAtLessThanEqual(
              MemberStatus.WITHDRAWN, threshold, pageable);

      int successCount = 0;
      for (Long memberId : memberIds) {
        // 회원 단위로 격리 — 한 건 실패가 나머지 익명화 처리를 막지 않도록 한다
        try {
          memberCommandService.anonymizeMember(memberId);
          successCount++;
        } catch (Exception e) {
          log.error("회원 익명화 처리 실패: memberId={}", memberId, e);
        }
      }

      // 배치 전체가 실패하면 대상 상태가 안 바뀌어서 다음 조회도 같은 회원들을 그대로
      // 돌려준다. 그대로 반복하면 무한 루프에 빠지므로, 이번 실행을 종료하고 다음
      // 스케줄에서 재시도한다.
      if (!memberIds.isEmpty() && successCount == 0) {
        log.warn("익명화 배치가 전부 실패해 이번 실행을 종료합니다. 다음 스케줄에서 재시도됩니다. 시도 건수={}", memberIds.size());
        break;
      }
    } while (memberIds.size() == BATCH_SIZE);
  }
}
