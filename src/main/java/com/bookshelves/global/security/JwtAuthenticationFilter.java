package com.bookshelves.global.security;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final MemberRepository memberRepository;
  private final RedisTokenRepository redisTokenRepository;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider,
      MemberRepository memberRepository,
      RedisTokenRepository redisTokenRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.memberRepository = memberRepository;
    this.redisTokenRepository = redisTokenRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = resolveToken(request);

    if (token != null && jwtTokenProvider.isValidToken(token, TokenType.ACCESS)) {
      Long memberId = jwtTokenProvider.getMemberId(token);

      if (isActiveMember(memberId)
          && !isRevokedByLogout(memberId, jwtTokenProvider.getIssuedAt(token))) {
        MemberPrincipal principal = new MemberPrincipal(memberId);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  // 탈퇴(WITHDRAWN)/정지(SUSPENDED)/익명화(ANONYMIZED) 상태는 access token이 아직 유효하더라도
  // 인증을 심지 않는다. WITHDRAWN은 계정복구 API로만, SUSPENDED는 어떤 API도 호출할 수 없어야 한다.
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

  private String resolveToken(HttpServletRequest request) {
    String authorization = request.getHeader(AUTHORIZATION_HEADER);

    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }

    return authorization.substring(BEARER_PREFIX.length());
  }
}
