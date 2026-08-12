package com.bookshelves.global.security;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// HTTP와 STOMP 인증에서 회원 상태와 로그아웃 시각을 동일하게 검증한다.
// WebSocket 설정과의 순환 의존을 피하기 위해 리포지토리에만 의존한다.
@Component
@RequiredArgsConstructor
public class AccessTokenGuard {

  private final MemberRepository memberRepository;
  private final RedisTokenRepository redisTokenRepository;

  public boolean grantsAccess(Long memberId, Instant tokenIssuedAt) {
    return isActiveMember(memberId) && !isRevokedByLogout(memberId, tokenIssuedAt);
  }

  // 활성 및 온보딩 중인 회원만 접근을 허용한다.
  private boolean isActiveMember(Long memberId) {
    return memberRepository
        .findStatusById(memberId)
        .map(status -> status == MemberStatus.ACTIVE || status == MemberStatus.PENDING_ONBOARDING)
        .orElse(false);
  }

  // 로그아웃 시각 이전에 발급된 토큰을 폐기한다.
  private boolean isRevokedByLogout(Long memberId, Instant tokenIssuedAt) {
    return redisTokenRepository
        .findLogoutAt(memberId)
        .map(logoutAt -> !tokenIssuedAt.isAfter(logoutAt))
        .orElse(false);
  }
}
