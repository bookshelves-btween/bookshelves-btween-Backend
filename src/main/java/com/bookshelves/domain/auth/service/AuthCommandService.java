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
import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.member.service.MemberCommandService;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.RedisTokenRepository;
import com.bookshelves.global.security.TokenType;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthCommandService {

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

  public void logout(Long memberId) {
    redisTokenRepository.deleteRefreshToken(memberId);
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

  private boolean isReissuable(MemberStatus status) {
    return status == MemberStatus.ACTIVE || status == MemberStatus.PENDING_ONBOARDING;
  }

  private ReissueResponse issueReissuedTokens(Member member, String oldRefreshToken) {
    TokenPair tokens = generateTokenPair(member.getId());

    // 검증(matches)과 회전(save) 사이 TOCTOU를 없애기 위해 Redis에서 원자적으로 비교 후 교체한다.
    // 동시에 같은 refresh token으로 재발급이 들어오면 하나만 성공하고 나머지는 거부된다.
    boolean rotated =
        redisTokenRepository.rotateRefreshToken(
            member.getId(),
            oldRefreshToken,
            tokens.refreshToken(),
            Duration.ofSeconds(tokens.refreshTokenExpiresIn()));

    if (!rotated) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_REFRESH_TOKEN);
    }

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
