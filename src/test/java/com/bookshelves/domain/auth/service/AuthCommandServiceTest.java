package com.bookshelves.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.auth.client.ProviderTokenVerifier;
import com.bookshelves.domain.auth.client.ProviderTokenVerifierResolver;
import com.bookshelves.domain.auth.client.ProviderUserInfo;
import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.config.JwtProperties;
import com.bookshelves.global.security.JwtTokenProvider;
import com.bookshelves.global.security.RedisTokenRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthCommandServiceTest {

  private final ProviderTokenVerifierResolver providerTokenVerifierResolver =
      mock(ProviderTokenVerifierResolver.class);
  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(
          new JwtProperties("bookshelves-test-jwt-secret-key-value", 3600, 1209600, 600));
  private final RedisTokenRepository redisTokenRepository = mock(RedisTokenRepository.class);
  private final AuthCommandService authCommandService =
      new AuthCommandService(
          providerTokenVerifierResolver, memberRepository, jwtTokenProvider, redisTokenRepository);

  @Test
  void newMemberIsCreatedAndIssuedTokens() {
    ProviderTokenVerifier verifier = stubVerifier("provider-token", "kakao-id");
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-id"))
        .thenReturn(Optional.empty());
    Member savedMember = mock(Member.class);
    when(savedMember.getId()).thenReturn(1L);
    when(savedMember.getStatus()).thenReturn(MemberStatus.PENDING_ONBOARDING);
    when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder()
                .provider(Provider.KAKAO)
                .providerToken("provider-token")
                .build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.PENDING_ONBOARDING);
    assertThat(response.getAccessToken()).isNotNull();
    assertThat(response.getRefreshToken()).isNotNull();
    assertThat(response.getRestoreToken()).isNull();
    verify(memberRepository).save(any(Member.class));
    verify(redisTokenRepository).saveRefreshToken(eq(1L), any(), eq(Duration.ofSeconds(1209600)));
  }

  @Test
  void existingActiveMemberIsIssuedTokensWithoutCreatingMember() {
    stubVerifier("provider-token", "kakao-id");
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(2L);
    when(member.getStatus()).thenReturn(MemberStatus.ACTIVE);
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-id"))
        .thenReturn(Optional.of(member));

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder()
                .provider(Provider.KAKAO)
                .providerToken("provider-token")
                .build());

    assertThat(response.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    assertThat(response.getAccessToken()).isNotNull();
    verify(memberRepository, never()).save(any());
  }

  @Test
  void withdrawnMemberIsIssuedRestoreTokenOnly() {
    stubVerifier("provider-token", "kakao-id");
    Member member = mock(Member.class);
    when(member.getId()).thenReturn(3L);
    when(member.getStatus()).thenReturn(MemberStatus.WITHDRAWN);
    when(member.getDeletedAt()).thenReturn(LocalDateTime.of(2026, 7, 14, 14, 30));
    when(memberRepository.findByProviderAndProviderId(Provider.KAKAO, "kakao-id"))
        .thenReturn(Optional.of(member));

    SocialLoginResponse response =
        authCommandService.socialLogin(
            SocialLoginRequest.builder()
                .provider(Provider.KAKAO)
                .providerToken("provider-token")
                .build());

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

  private ProviderTokenVerifier stubVerifier(String providerToken, String providerId) {
    ProviderTokenVerifier verifier = mock(ProviderTokenVerifier.class);
    when(providerTokenVerifierResolver.resolve(Provider.KAKAO)).thenReturn(verifier);
    when(verifier.verify(providerToken)).thenReturn(new ProviderUserInfo(providerId));
    return verifier;
  }
}
