package com.bookshelves.domain.auth.service;

import com.bookshelves.domain.auth.client.ProviderTokenVerifier;
import com.bookshelves.domain.auth.client.ProviderTokenVerifierResolver;
import com.bookshelves.domain.auth.client.ProviderUserInfo;
import com.bookshelves.domain.auth.converter.AuthConverter;
import com.bookshelves.domain.auth.dto.request.ReissueRequest;
import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.ReissueResponse;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.member.service.MemberCommandService;
import com.bookshelves.global.exception.ProjectException;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.RedisTokenRepository;
import com.bookshelves.global.security.TokenType;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthCommandService {

  private static final long RESTORE_PERIOD_DAYS = 30;
  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  private final ProviderTokenVerifierResolver providerTokenVerifierResolver;
  private final MemberRepository memberRepository;
  private final MemberCommandService memberCommandService;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisTokenRepository redisTokenRepository;

  public AuthCommandService(
      ProviderTokenVerifierResolver providerTokenVerifierResolver,
      MemberRepository memberRepository,
      MemberCommandService memberCommandService,
      JwtTokenProvider jwtTokenProvider,
      RedisTokenRepository redisTokenRepository) {
    this.providerTokenVerifierResolver = providerTokenVerifierResolver;
    this.memberRepository = memberRepository;
    this.memberCommandService = memberCommandService;
    this.jwtTokenProvider = jwtTokenProvider;
    this.redisTokenRepository = redisTokenRepository;
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

  public ReissueResponse reissue(ReissueRequest request) {
    String refreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.isValidToken(refreshToken, TokenType.REFRESH)) {
      throw new ProjectException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    Long memberId = jwtTokenProvider.getMemberId(refreshToken);

    if (!redisTokenRepository.matchesRefreshToken(memberId, refreshToken)) {
      throw new ProjectException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

    Member member =
        memberRepository
            .findById(memberId)
            .orElseThrow(() -> new ProjectException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN));

    if (!isReissuable(member.getStatus())) {
      throw new ProjectException(AuthErrorCode.AUTH_UNREISSUABLE_MEMBER_STATUS);
    }

    return issueReissuedTokens(member);
  }

  private boolean isReissuable(MemberStatus status) {
    return status == MemberStatus.ACTIVE || status == MemberStatus.PENDING_ONBOARDING;
  }

  private ReissueResponse issueReissuedTokens(Member member) {
    TokenPair tokens = issueTokenPair(member);

    return AuthConverter.toReissueResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.accessTokenExpiresIn(),
        tokens.refreshTokenExpiresIn());
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
      throw new ProjectException(AuthErrorCode.AUTH_UNSUPPORTED_PROVIDER);
    }
  }

  private SocialLoginResponse issueLoginTokens(Member member) {
    TokenPair tokens = issueTokenPair(member);

    return AuthConverter.toSocialLoginTokenResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.accessTokenExpiresIn(),
        tokens.refreshTokenExpiresIn(),
        member.getStatus());
  }

  private TokenPair issueTokenPair(Member member) {
    long accessTokenExpiresIn = jwtTokenProvider.getExpirationSeconds(TokenType.ACCESS);
    long refreshTokenExpiresIn = jwtTokenProvider.getExpirationSeconds(TokenType.REFRESH);
    String accessToken = jwtTokenProvider.generateToken(member.getId(), TokenType.ACCESS);
    String refreshToken = jwtTokenProvider.generateToken(member.getId(), TokenType.REFRESH);

    redisTokenRepository.saveRefreshToken(
        member.getId(), refreshToken, Duration.ofSeconds(refreshTokenExpiresIn));

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
        member.getDeletedAt().plusDays(RESTORE_PERIOD_DAYS).atZone(SERVICE_ZONE).toOffsetDateTime();

    return AuthConverter.toSocialLoginWithdrawnResponse(
        restoreToken, restoreTokenExpiresIn, scheduledDeletionAt);
  }
}
