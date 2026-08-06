package com.bookshelves.global.security;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 서명이 유효한 access token이라도 그 주체가 지금도 접근 자격을 갖는지는 별개다.
// 회원 상태와 로그아웃 시각을 함께 본다.
//
// HTTP 필터(JwtAuthenticationFilter)와 STOMP CONNECT 인터셉터(StompAuthChannelInterceptor)가
// 같은 인스턴스를 쓴다. 판정을 각자 들고 있으면 한쪽에만 조건이 추가돼 같은 회원이 HTTP는 막히고
// 웹소켓은 통과하는 비대칭이 생긴다.
//
// 리포지토리에만 의존해야 한다. 인터셉터가 WebSocketConfig 의존 사슬 위에 있어 서비스 계층을 물면
// 부팅이 실패한다. ChatSubscriptionValidator와 같은 제약이다.
@Component
@RequiredArgsConstructor
public class AccessTokenGuard {

  private final MemberRepository memberRepository;
  private final RedisTokenRepository redisTokenRepository;

  public boolean grantsAccess(Long memberId, Instant tokenIssuedAt) {
    return isActiveMember(memberId) && !isRevokedByLogout(memberId, tokenIssuedAt);
  }

  // 탈퇴(WITHDRAWN)/정지(SUSPENDED)/익명화(ANONYMIZED) 상태는 access token이 아직 유효하더라도
  // 통과시키지 않는다. WITHDRAWN은 계정복구 API로만, SUSPENDED는 어떤 API도 호출할 수 없어야 한다.
  private boolean isActiveMember(Long memberId) {
    return memberRepository
        .findStatusById(memberId)
        .map(status -> status == MemberStatus.ACTIVE || status == MemberStatus.PENDING_ONBOARDING)
        .orElse(false);
  }

  // JWT는 stateless라 로그아웃해도 access token 자체는 만료 전까지 서명이 유효하다. 로그아웃
  // 시각보다 먼저 발급된 토큰은 여기서 걸러내, 로그아웃 이후에도 캡처된 토큰이 계속 통하는
  // 것을 막는다.
  private boolean isRevokedByLogout(Long memberId, Instant tokenIssuedAt) {
    return redisTokenRepository
        .findLogoutAt(memberId)
        .map(logoutAt -> !tokenIssuedAt.isAfter(logoutAt))
        .orElse(false);
  }
}
