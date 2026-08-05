package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  private static final String SECRET = "bookshelves-test-jwt-secret-key-value";

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(new JwtProperties(SECRET, 3600, 1209600, 600));
  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final RedisTokenRepository redisTokenRepository = mock(RedisTokenRepository.class);
  private final JwtAuthenticationFilter jwtAuthenticationFilter =
      new JwtAuthenticationFilter(jwtTokenProvider, memberRepository, redisTokenRepository);

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void validAccessTokenSetsMemberPrincipalWhenMemberIsActive()
      throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.ACTIVE));
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new MemberPrincipal(1L));
  }

  @Test
  void validAccessTokenSetsMemberPrincipalWhenMemberIsPendingOnboarding()
      throws ServletException, IOException {
    when(memberRepository.findStatusById(1L))
        .thenReturn(Optional.of(MemberStatus.PENDING_ONBOARDING));
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new MemberPrincipal(1L));
  }

  @Test
  void withdrawnMemberDoesNotGetAuthenticated() throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.WITHDRAWN));
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void suspendedMemberDoesNotGetAuthenticated() throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.SUSPENDED));
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void unknownMemberDoesNotGetAuthenticated() throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.empty());
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void accessTokenIssuedBeforeLogoutDoesNotGetAuthenticated() throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.ACTIVE));
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    // 이 토큰이 발급된 이후 시점에 로그아웃함 — 이 토큰은 로그아웃 이전에 발급된 것으로 간주돼야 한다
    when(redisTokenRepository.findLogoutAt(1L))
        .thenReturn(Optional.of(jwtTokenProvider.getIssuedAt(token).plusSeconds(1)));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void accessTokenIssuedAfterLogoutStillGetsAuthenticated() throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.ACTIVE));
    // 로그아웃 이후 재로그인해서 새로 발급받은 토큰 — 이 토큰까지 막히면 안 된다
    when(redisTokenRepository.findLogoutAt(1L))
        .thenReturn(Optional.of(Instant.now().minusSeconds(60)));
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new MemberPrincipal(1L));
  }

  @Test
  void accessTokenIssuedInTheSameSecondAsLogoutButAfterItStillGetsAuthenticated()
      throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.ACTIVE));
    // 로그아웃(현재 초의 .500)과 재로그인(같은 초의 .900)이 같은 초 안에서 벌어진 상황을
    // 밀리초 단위로 정밀하게 재현한다. 만료 검사는 실제 벽시계 기준이라 임의의 과거/미래
    // 날짜를 쓰면 안 되므로, 현재 초를 기준으로 삼는다. iat을 초 단위로만 비교하면 .900에
    // 발급된 이 토큰의 iat이 그 초의 시작(.000)으로 잘려 로그아웃 시각(.500)보다 이전으로
    // 보이고, 방금 재로그인한 정상 토큰이 폐기된 토큰으로 오인된다.
    Instant currentSecond = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant tokenIssuedAt = currentSecond.plusMillis(900);
    Instant logoutAt = currentSecond.plusMillis(500);
    JwtTokenProvider fixedClockTokenProvider =
        new JwtTokenProvider(
            new JwtProperties(SECRET, 3600, 1209600, 600),
            Clock.fixed(tokenIssuedAt, ZoneOffset.UTC));
    String token = fixedClockTokenProvider.generateToken(1L, TokenType.ACCESS);
    when(redisTokenRepository.findLogoutAt(1L)).thenReturn(Optional.of(logoutAt));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    assertThat(principal).isEqualTo(new MemberPrincipal(1L));
  }

  @Test
  void legacyAccessTokenWithoutIssuedAtMillisClaimDoesNotAuthenticateAndDoesNotThrow()
      throws ServletException, IOException {
    when(memberRepository.findStatusById(1L)).thenReturn(Optional.of(MemberStatus.ACTIVE));
    // issuedAtMillis 클레임 도입 이전에 발급된, 서명은 유효한 토큰을 흉내낸다. 이 클레임이
    // 없으면 getIssuedAt()에서 NPE가 나 요청이 500으로 끝나던 문제를 검증한다 —
    // isValidToken이 미리 걸러 예외 없이 401(미인증)로 처리돼야 한다.
    SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    String legacyToken =
        Jwts.builder()
            .subject("1")
            .claim("memberId", 1L)
            .claim("tokenType", TokenType.ACCESS.name())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(secretKey)
            .compact();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + legacyToken);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void refreshTokenDoesNotSetAuthentication() throws ServletException, IOException {
    String token = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
