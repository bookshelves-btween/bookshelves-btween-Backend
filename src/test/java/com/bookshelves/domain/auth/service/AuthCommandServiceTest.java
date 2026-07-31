package com.bookshelves.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.auth.client.ProviderTokenVerifier;
import com.bookshelves.domain.auth.client.ProviderTokenVerifierResolver;
import com.bookshelves.domain.auth.client.ProviderUserInfo;
import com.bookshelves.domain.auth.dto.request.FakeSignUpRequest;
import com.bookshelves.domain.auth.dto.request.ReissueRequest;
import com.bookshelves.domain.auth.dto.request.RestoreRequest;
import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.ReissueResponse;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.member.service.MemberCommandService;
import com.bookshelves.global.config.JwtProperties;
import com.bookshelves.global.exception.ProjectException;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.RedisTokenRepository;
import com.bookshelves.global.security.TokenType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataIntegrityViolationException;

class AuthCommandServiceTest {

  private static final String FAKE_SECRET = "fake-sign-up-secret-for-test";

  private final ProviderTokenVerifierResolver providerTokenVerifierResolver =
      mock(ProviderTokenVerifierResolver.class);
  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final MemberCommandService memberCommandService = mock(MemberCommandService.class);
  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(
          new JwtProperties("bookshelves-test-jwt-secret-key-value", 3600, 1209600, 600));
  private final RedisTokenRepository redisTokenRepository = mock(RedisTokenRepository.class);
  private final AuthCommandService authCommandService =
      new AuthCommandService(
          providerTokenVerifierResolver,
          memberRepository,
          memberCommandService,
          jwtTokenProvider,
          redisTokenRepository,
          FAKE_SECRET);

  @ParameterizedTest
  @EnumSource(Provider.class)
  void newMemberIsCreatedAndIssuedTokens(Provider provider) {
    String providerId = provider.name().toLowerCase() + "-id";
    stubVerifier(provider, "provider-token", providerId);
    when(memberRepository.findByProviderAndProviderId(provider, providerId))
        .thenReturn(Optional.empty());
    Member savedMember = mock(Member.class);
    when(savedMember.getId()).thenReturn(1L);
    when(savedMember.getStatus()).thenReturn(MemberStatus.PENDING_ONBOARDING);
    when(memberCommandService.createSocialMember(provider, providerId)).thenReturn(savedMember);

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder()
                .provider(provider.name())
                .providerToken("provider-token")
                .build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.PENDING_ONBOARDING);
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    assertThat(response.getRestoreToken()).isNull();
    verify(memberCommandService).createSocialMember(provider, providerId);
    verify(redisTokenRepository).saveRefreshToken(eq(1L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void fakeSignUpCreatesActiveMemberAndIssuesTokens() {
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "fake-tester-1"))
        .thenReturn(Optional.empty());
    Member savedMember = mock(Member.class);
    when(savedMember.getId()).thenReturn(7L);
    when(savedMember.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberCommandService.createSocialMember(Provider.KAKAO, "fake-tester-1"))
        .thenReturn(savedMember);

    SocialLoginResponse response =
        authCommandService.fakeSignUp(
            FakeSignUpRequest.builder().key("tester-1").secret(FAKE_SECRET).build());

    // 온보딩을 건너뛰고 바로 쓸 수 있어야 한다
    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    verify(savedMember).updateNickname("tester-1", "테스트", "계정");
    verify(savedMember).completeOnboarding();
    verify(redisTokenRepository).saveRefreshToken(eq(7L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void fakeSignUpWithSameKeyLogsInAsTheSameMember() {
    Member existingMember = mock(Member.class);
    when(existingMember.getId()).thenReturn(7L);
    when(existingMember.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "fake-tester-1"))
        .thenReturn(Optional.of(existingMember));

    SocialLoginResponse response =
        authCommandService.fakeSignUp(
            FakeSignUpRequest.builder().key("tester-1").secret(FAKE_SECRET).build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    // 같은 key는 회원을 새로 만들지 않는다 — 재로그인해도 참여한 모임이 유지되어야 한다
    verify(memberCommandService, never()).createSocialMember(any(), any());
    verify(redisTokenRepository).saveRefreshToken(eq(7L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void fakeSignUpNeverReachesRealSocialMembers() {
    // provider_id에 fake- 접두사가 붙으므로 실제 카카오 회원(숫자 provider_id)과 겹치지 않는다
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "fake-tester-2"))
        .thenReturn(Optional.empty());
    Member savedMember = mock(Member.class);
    when(savedMember.getId()).thenReturn(8L);
    when(savedMember.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberCommandService.createSocialMember(Provider.KAKAO, "fake-tester-2"))
        .thenReturn(savedMember);

    authCommandService.fakeSignUp(
        FakeSignUpRequest.builder().key("tester-2").secret(FAKE_SECRET).build());

    verify(memberRepository).findByProviderAndProviderId(Provider.KAKAO, "fake-tester-2");
    verify(memberRepository, never()).findByProviderAndProviderId(Provider.KAKAO, "tester-2");
  }

  @Test
  void fakeSignUpRejectsWrongSecretWithoutTouchingMembers() {
    assertThatThrownBy(
            () ->
                authCommandService.fakeSignUp(
                    FakeSignUpRequest.builder().key("tester-1").secret("wrong-secret").build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_FAKE_SIGN_UP_SECRET);

    verify(memberRepository, never()).findByProviderAndProviderId(any(), any());
    verify(memberCommandService, never()).createSocialMember(any(), any());
  }

  @Test
  void fakeSignUpIsClosedWhenServerHasNoSecretConfigured() {
    // 설정을 빠뜨린 환경에서 열린 채로 남지 않아야 한다
    AuthCommandService serviceWithoutSecret =
        new AuthCommandService(
            providerTokenVerifierResolver,
            memberRepository,
            memberCommandService,
            jwtTokenProvider,
            redisTokenRepository,
            "");

    assertThatThrownBy(
            () ->
                serviceWithoutSecret.fakeSignUp(
                    FakeSignUpRequest.builder().key("tester-1").secret("anything").build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_FAKE_SIGN_UP_SECRET);

    verify(memberRepository, never()).findByProviderAndProviderId(any(), any());
  }

  @Test
  void concurrentFirstLoginFallsBackToExistingMemberOnUniqueConstraintViolation() {
    stubVerifier(Provider.KAKAO, "provider-token", "kakao-id");
    Member memberCreatedByConcurrentRequest = mock(Member.class);
    when(memberCreatedByConcurrentRequest.getId()).thenReturn(1L);
    when(memberCreatedByConcurrentRequest.getStatus()).thenReturn(MemberStatus.PENDING_ONBOARDING);
    // 최초 조회는 비어있지만(둘 다 신규로 판단), 저장 시점엔 동시 요청이 먼저 커밋해서 유니크 제약 위반이 남
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-id"))
        .thenReturn(Optional.empty(), Optional.of(memberCreatedByConcurrentRequest));
    when(memberCommandService.createSocialMember(Provider.KAKAO, "kakao-id"))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder().provider("KAKAO").providerToken("provider-token").build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.PENDING_ONBOARDING);
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    verify(memberRepository, times(2)).findByProviderAndProviderId(Provider.KAKAO, "kakao-id");
    verify(redisTokenRepository).saveRefreshToken(eq(1L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void existingActiveMemberIsIssuedTokensWithoutCreatingMember() {
    stubVerifier(Provider.KAKAO, "provider-token", "kakao-id");
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(2L);
    when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-id"))
        .thenReturn(Optional.of(member));

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder().provider("KAKAO").providerToken("provider-token").build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    verify(memberCommandService, never()).createSocialMember(Provider.KAKAO, "kakao-id");
    verify(redisTokenRepository).saveRefreshToken(eq(2L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void withdrawnMemberIsIssuedRestoreTokenOnly() {
    stubVerifier(Provider.KAKAO, "provider-token", "kakao-id");
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(3L);
    when(member.getStatus()).thenReturn(MemberStatus.WITHDRAWN);
    when(member.getDeletedAt()).thenReturn(LocalDateTime.of(2026, 7, 14, 14, 30));
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-id"))
        .thenReturn(Optional.of(member));

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder().provider("KAKAO").providerToken("provider-token").build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    assertThat(response.getAccessToken()).isNull();
    assertThat(response.getRefreshToken()).isNull();
    assertThat(response.getRestoreToken()).isNotNull();
    assertThat(response.getScheduledDeletionAt())
        .isEqualTo(
            LocalDateTime.of(2026, 8, 13, 14, 30)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toOffsetDateTime());
    verify(redisTokenRepository).saveRestoreToken(eq(3L), any(), eq(Duration.ofSeconds(600)));
  }

  @Test
  void logoutDeletesRefreshToken() {
    authCommandService.logout(1L);

    verify(redisTokenRepository).deleteRefreshToken(1L);
  }

  @Test
  void socialLoginThrowsUnsupportedProviderForUnknownProviderString() {
    SocialLoginRequest request =
        SocialLoginRequest.builder().provider("NAVER").providerToken("provider-token").build();

    assertThatThrownBy(() -> authCommandService.socialLogin(request))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_UNSUPPORTED_PROVIDER);
  }

  @Test
  void reissueSucceedsAndRotatesRefreshToken() {
    String oldRefreshToken = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(1L);
    when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    when(redisTokenRepository.rotateRefreshToken(eq(1L), eq(oldRefreshToken), any(), any()))
        .thenReturn(true);

    ReissueResponse response =
        authCommandService.reissue(ReissueRequest.builder().refreshToken(oldRefreshToken).build());

    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    assertThat(jwtTokenProvider.isValidToken(response.getRefreshToken(), TokenType.REFRESH))
        .isTrue();
    // jti로 토큰마다 고유성이 보장되므로 새 refreshToken은 항상 이전 값과 달라야 한다.
    assertThat(response.getRefreshToken()).isNotEqualTo(oldRefreshToken);
    verify(redisTokenRepository)
        .rotateRefreshToken(
            eq(1L),
            eq(oldRefreshToken),
            eq(response.getRefreshToken()),
            eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void reissueThrowsInvalidRefreshTokenForWrongTokenType() {
    String accessToken = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);

    assertThatThrownBy(
            () ->
                authCommandService.reissue(
                    ReissueRequest.builder().refreshToken(accessToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    verify(memberRepository, never()).findById(any());
    verify(redisTokenRepository, never()).rotateRefreshToken(any(), any(), any(), any());
  }

  @Test
  void reissueThrowsInvalidRefreshTokenWhenRotationLosesRace() {
    String oldRefreshToken = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(1L);
    when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
    // 동시에 같은 refresh token으로 다른 요청이 먼저 원자적으로 회전시킨 상황(CAS 실패)을 재현
    when(redisTokenRepository.rotateRefreshToken(eq(1L), eq(oldRefreshToken), any(), any()))
        .thenReturn(false);

    assertThatThrownBy(
            () ->
                authCommandService.reissue(
                    ReissueRequest.builder().refreshToken(oldRefreshToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
  }

  @Test
  void reissueThrowsInvalidRefreshTokenWhenMemberNotFound() {
    String refreshToken = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    when(memberRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                authCommandService.reissue(
                    ReissueRequest.builder().refreshToken(refreshToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    verify(redisTokenRepository, never()).rotateRefreshToken(any(), any(), any(), any());
  }

  @Test
  void reissueThrowsUnreissuableMemberStatusForWithdrawnMember() {
    String refreshToken = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    Member member = mock(Member.class);
    when(member.getStatus()).thenReturn(MemberStatus.WITHDRAWN);
    when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

    assertThatThrownBy(
            () ->
                authCommandService.reissue(
                    ReissueRequest.builder().refreshToken(refreshToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_UNREISSUABLE_MEMBER_STATUS);
    verify(redisTokenRepository, never()).rotateRefreshToken(any(), any(), any(), any());
  }

  @Test
  void restoreSucceedsAndReissuesTokens() {
    String restoreToken = jwtTokenProvider.generateToken(5L, TokenType.RESTORE);
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(5L);
    when(member.getStatus()).thenReturn(MemberStatus.WITHDRAWN, MemberStatus.ACTIVE);
    when(member.getDeletedAt()).thenReturn(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(5));
    when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
    when(redisTokenRepository.consumeRestoreToken(5L, restoreToken)).thenReturn(true);

    SocialLoginResponse response =
        authCommandService.restore(RestoreRequest.builder().restoreToken(restoreToken).build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    verify(member).restore();
    verify(redisTokenRepository).saveRefreshToken(eq(5L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void restoreThrowsInvalidRestoreTokenForWrongTokenType() {
    String accessToken = jwtTokenProvider.generateToken(5L, TokenType.ACCESS);

    assertThatThrownBy(
            () ->
                authCommandService.restore(
                    RestoreRequest.builder().restoreToken(accessToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_RESTORE_TOKEN);
    verify(memberRepository, never()).findById(any());
  }

  @Test
  void restoreThrowsInvalidRestoreTokenWhenMemberNotFound() {
    String restoreToken = jwtTokenProvider.generateToken(5L, TokenType.RESTORE);
    when(memberRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                authCommandService.restore(
                    RestoreRequest.builder().restoreToken(restoreToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_RESTORE_TOKEN);
  }

  @Test
  void restoreThrowsUnrestorableMemberStatusWhenNotWithdrawn() {
    String restoreToken = jwtTokenProvider.generateToken(5L, TokenType.RESTORE);
    Member member = mock(Member.class);
    when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberRepository.findById(5L)).thenReturn(Optional.of(member));

    assertThatThrownBy(
            () ->
                authCommandService.restore(
                    RestoreRequest.builder().restoreToken(restoreToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_UNRESTORABLE_MEMBER_STATUS);
    verify(redisTokenRepository, never()).consumeRestoreToken(any(), any());
  }

  @Test
  void restoreThrowsRestorePeriodExpiredWhenPastThirtyDays() {
    String restoreToken = jwtTokenProvider.generateToken(5L, TokenType.RESTORE);
    Member member = mock(Member.class);
    when(member.getStatus()).thenReturn(MemberStatus.WITHDRAWN);
    when(member.getDeletedAt())
        .thenReturn(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(31));
    when(memberRepository.findById(5L)).thenReturn(Optional.of(member));

    assertThatThrownBy(
            () ->
                authCommandService.restore(
                    RestoreRequest.builder().restoreToken(restoreToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_RESTORE_PERIOD_EXPIRED);
    verify(redisTokenRepository, never()).consumeRestoreToken(any(), any());
  }

  @Test
  void restoreThrowsInvalidRestoreTokenWhenRedisDoesNotMatch() {
    String restoreToken = jwtTokenProvider.generateToken(5L, TokenType.RESTORE);
    Member member = mock(Member.class);
    when(member.getStatus()).thenReturn(MemberStatus.WITHDRAWN);
    when(member.getDeletedAt()).thenReturn(LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(5));
    when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
    when(redisTokenRepository.consumeRestoreToken(5L, restoreToken)).thenReturn(false);

    assertThatThrownBy(
            () ->
                authCommandService.restore(
                    RestoreRequest.builder().restoreToken(restoreToken).build()))
        .isInstanceOf(ProjectException.class)
        .extracting(e -> ((ProjectException) e).getErrorCode())
        .isEqualTo(AuthErrorCode.AUTH_INVALID_RESTORE_TOKEN);
    verify(member, never()).restore();
  }

  private ProviderTokenVerifier stubVerifier(
      Provider provider, String providerToken, String providerId) {
    ProviderTokenVerifier verifier = mock(ProviderTokenVerifier.class);
    when(providerTokenVerifierResolver.resolve(provider)).thenReturn(verifier);
    when(verifier.verify(providerToken)).thenReturn(new ProviderUserInfo(providerId));
    return verifier;
  }
}
