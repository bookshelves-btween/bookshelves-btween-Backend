package com.bookshelves.domain.auth.service;

import com.bookshelves.domain.auth.client.ProviderTokenVerifier;
import com.bookshelves.domain.auth.client.ProviderTokenVerifierResolver;
import com.bookshelves.domain.auth.client.ProviderUserInfo;
import com.bookshelves.domain.auth.converter.AuthConverter;
import com.bookshelves.domain.auth.dto.request.FakeSignUpRequest;
import com.bookshelves.domain.auth.dto.request.ReissueRequest;
import com.bookshelves.domain.auth.dto.request.RestoreRequest;
import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.ReissueResponse;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.member.service.MemberCommandService;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.RedisTokenRepository;
import com.bookshelves.global.security.TokenType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthCommandService {

  // 실제 카카오 provider ID와 겹치지 않도록 테스트 회원에 접두사를 붙인다.
  private static final Provider FAKE_PROVIDER = Provider.KAKAO;
  private static final String FAKE_PROVIDER_ID_PREFIX = "fake-";

  private final ProviderTokenVerifierResolver providerTokenVerifierResolver;
  private final MemberRepository memberRepository;
  private final MemberCommandService memberCommandService;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisTokenRepository redisTokenRepository;
  private final String fakeSignUpSecret;

  public AuthCommandService(
      ProviderTokenVerifierResolver providerTokenVerifierResolver,
      MemberRepository memberRepository,
      MemberCommandService memberCommandService,
      JwtTokenProvider jwtTokenProvider,
      RedisTokenRepository redisTokenRepository,
      @Value("${auth.fake-sign-up-secret:}") String fakeSignUpSecret) {
    this.providerTokenVerifierResolver = providerTokenVerifierResolver;
    this.memberRepository = memberRepository;
    this.memberCommandService = memberCommandService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.redisTokenRepository = redisTokenRepository;
    this.fakeSignUpSecret = fakeSignUpSecret;
  }

  public SocialLoginResponse socialLogin(SocialLoginRequest request) {
    Provider provider = parseProvider(request.getProvider());
    ProviderTokenVerifier verifier = providerTokenVerifierResolver.resolve(provider);
    ProviderUserInfo providerUserInfo = verifier.verify(request.getProviderToken());

    Member member =
        memberRepository
            .findByProviderAndProviderId(provider, providerUserInfo.providerId())
            .orElseGet(() -> createSocialMember(provider, providerUserInfo.providerId()));

    if (member.getStatus() == MemberStatus.WITHDRAWN) {
      return issueRestoreToken(member);
    }

    return issueLoginTokens(member);
  }

  // 서버 비밀값으로 보호되는 임시 테스트 토큰 발급 경로이며 앱 공개 전에 제거한다.
  public SocialLoginResponse fakeSignUp(FakeSignUpRequest request) {
    validateFakeSignUpSecret(request.getSecret());

    String providerId = FAKE_PROVIDER_ID_PREFIX + request.getKey();

    Member member =
        memberRepository
            .findByProviderAndProviderId(FAKE_PROVIDER, providerId)
            .orElseGet(() -> createFakeMember(providerId, request.getKey()));

    return issueLoginTokens(member);
  }

  // 비밀값이 설정되지 않은 환경에서는 모든 요청을 거부한다.
  private void validateFakeSignUpSecret(String secret) {
    if (fakeSignUpSecret == null || fakeSignUpSecret.isBlank()) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_FAKE_SIGN_UP_SECRET);
    }

    // 상수 시간 비교로 비밀값 추측을 어렵게 한다.
    if (!MessageDigest.isEqual(
        fakeSignUpSecret.getBytes(StandardCharsets.UTF_8),
        secret.getBytes(StandardCharsets.UTF_8))) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_FAKE_SIGN_UP_SECRET);
    }
  }

  // 테스트 회원은 온보딩을 마친 ACTIVE 상태로 생성한다.
  private Member createFakeMember(String providerId, String key) {
    return memberCommandService.createAndOnboardFakeMember(
        FAKE_PROVIDER, providerId, key, ProfileBackgroundColor.GREEN);
  }

  public void logout(Long memberId) {
    redisTokenRepository.deleteRefreshToken(memberId);
    redisTokenRepository.saveLogoutAt(
        memberId, Duration.ofSeconds(jwtTokenProvider.getExpirationSeconds(TokenType.ACCESS)));
  }

  public ReissueResponse reissue(ReissueRequest request) {
    String oldRefreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.isValidToken(oldRefreshToken, TokenType.REFRESH)) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    Long memberId = jwtTokenProvider.getMemberId(oldRefreshToken);

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN));

    if (!isReissuable(member.getStatus())) {
      throw new AuthException(AuthErrorCode.AUTH_UNREISSUABLE_MEMBER_STATUS);
    }

    return issueReissuedTokens(member, oldRefreshToken);
  }

  public SocialLoginResponse restore(RestoreRequest request) {
    String restoreToken = request.getRestoreToken();

    if (!jwtTokenProvider.isValidToken(restoreToken, TokenType.RESTORE)) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_RESTORE_TOKEN);
    }

    Long memberId = jwtTokenProvider.getMemberId(restoreToken);

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_INVALID_RESTORE_TOKEN));

    if (member.getStatus() != MemberStatus.WITHDRAWN) {
      throw new AuthException(AuthErrorCode.AUTH_UNRESTORABLE_MEMBER_STATUS);
    }

    if (isRestorePeriodExpired(member)) {
      throw new AuthException(AuthErrorCode.AUTH_RESTORE_PERIOD_EXPIRED);
    }

    // Redis에서 복구 토큰을 원자적으로 검증하고 소모한다.
    if (!redisTokenRepository.consumeRestoreToken(memberId, restoreToken)) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_RESTORE_TOKEN);
    }

    member.restore();

    return issueLoginTokens(member);
  }

  private boolean isRestorePeriodExpired(Member member) {
    LocalDateTime restoreDeadline = member.getDeletedAt().plusDays(Member.RESTORE_PERIOD_DAYS);
    return LocalDateTime.now(Member.SERVICE_ZONE).isAfter(restoreDeadline);
  }

  private boolean isReissuable(MemberStatus status) {
    return status == MemberStatus.ACTIVE || status == MemberStatus.PENDING_ONBOARDING;
  }

  private ReissueResponse issueReissuedTokens(Member member, String oldRefreshToken) {
    TokenPair tokens = generateTokenPair(member.getId());

    // refresh token을 원자적으로 회전하고 동시 요청에는 짧은 grace 결과를 공유한다.
    boolean rotated =
        redisTokenRepository.rotateRefreshToken(
            member.getId(),
            oldRefreshToken,
            tokens.refreshToken(),
            Duration.ofSeconds(tokens.refreshTokenExpiresIn()),
            buildGracePayload(tokens));

    if (!rotated) {
      Optional<String> gracePayload =
          redisTokenRepository.findRotationGracePayload(member.getId(), oldRefreshToken);
      if (gracePayload.isPresent()) {
        return parseGracePayload(gracePayload.get());
      }

      // grace에 없는 구 토큰의 재사용은 탈취로 간주해 현재 세션도 무효화한다.
      log.warn("회전된 refresh token 재사용 의심으로 세션을 무효화합니다. memberId={}", member.getId());
      redisTokenRepository.deleteRefreshToken(member.getId());
      throw new AuthException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    return AuthConverter.toReissueResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.accessTokenExpiresIn(),
        tokens.refreshTokenExpiresIn());
  }

  private String buildGracePayload(TokenPair tokens) {
    return tokens.accessToken()
        + '|'
        + tokens.refreshToken()
        + '|'
        + tokens.accessTokenExpiresIn()
        + '|'
        + tokens.refreshTokenExpiresIn();
  }

  private ReissueResponse parseGracePayload(String gracePayload) {
    String[] parts = gracePayload.split("\\|", 4);
    return AuthConverter.toReissueResponse(
        parts[0], parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]));
  }

  private Member createSocialMember(Provider provider, String providerId) {
    try {
      return memberCommandService.createSocialMember(provider, providerId);
    } catch (DataIntegrityViolationException e) {
      return memberRepository
          .findByProviderAndProviderId(provider, providerId)
          .orElseThrow(() -> e);
    }
  }

  private Provider parseProvider(String provider) {
    try {
      return Provider.valueOf(provider);
    } catch (IllegalArgumentException e) {
      throw new AuthException(AuthErrorCode.AUTH_UNSUPPORTED_PROVIDER);
    }
  }

  private SocialLoginResponse issueLoginTokens(Member member) {
    TokenPair tokens = generateTokenPair(member.getId());

    redisTokenRepository.saveRefreshToken(
        member.getId(), tokens.refreshToken(), Duration.ofSeconds(tokens.refreshTokenExpiresIn()));

    return AuthConverter.toSocialLoginTokenResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.accessTokenExpiresIn(),
        tokens.refreshTokenExpiresIn(),
        member.getStatus());
  }

  private TokenPair generateTokenPair(Long memberId) {
    long accessTokenExpiresIn = jwtTokenProvider.getExpirationSeconds(TokenType.ACCESS);
    long refreshTokenExpiresIn = jwtTokenProvider.getExpirationSeconds(TokenType.REFRESH);
    String accessToken = jwtTokenProvider.generateToken(memberId, TokenType.ACCESS);
    String refreshToken = jwtTokenProvider.generateToken(memberId, TokenType.REFRESH);

    return new TokenPair(accessToken, refreshToken, accessTokenExpiresIn, refreshTokenExpiresIn);
  }

  private record TokenPair(
      String accessToken,
      String refreshToken,
      long accessTokenExpiresIn,
      long refreshTokenExpiresIn) {}

  private SocialLoginResponse issueRestoreToken(Member member) {
    long restoreTokenExpiresIn = jwtTokenProvider.getExpirationSeconds(TokenType.RESTORE);
    String restoreToken = jwtTokenProvider.generateToken(member.getId(), TokenType.RESTORE);

    redisTokenRepository.saveRestoreToken(
        member.getId(), restoreToken, Duration.ofSeconds(restoreTokenExpiresIn));

    OffsetDateTime scheduledDeletionAt =
        member
            .getDeletedAt()
            .plusDays(Member.RESTORE_PERIOD_DAYS)
            .atZone(Member.SERVICE_ZONE)
            .toOffsetDateTime();

    return AuthConverter.toSocialLoginWithdrawnResponse(
        restoreToken, restoreTokenExpiresIn, scheduledDeletionAt);
  }
}
