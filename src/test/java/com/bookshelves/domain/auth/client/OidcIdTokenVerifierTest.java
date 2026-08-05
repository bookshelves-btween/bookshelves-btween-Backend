package com.bookshelves.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.global.exception.ProjectException;
import com.bookshelves.global.security.RedisTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class OidcIdTokenVerifierTest {

  private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
  private final RedisTokenRepository redisTokenRepository = mock(RedisTokenRepository.class);
  private final OidcIdTokenVerifier oidcIdTokenVerifier =
      new OidcIdTokenVerifier(redisTokenRepository);

  @Test
  void verifySubjectReturnsSubjectClaimForValidUnusedToken() {
    Jwt jwt =
        Jwt.withTokenValue("id-token")
            .header("alg", "none")
            .claim("sub", "provider-user-id")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    when(jwtDecoder.decode("id-token")).thenReturn(jwt);
    when(redisTokenRepository.consumeOidcIdToken(eq("id-token"), any())).thenReturn(true);

    String subject = oidcIdTokenVerifier.verifySubject(jwtDecoder, "id-token");

    assertThat(subject).isEqualTo("provider-user-id");
  }

  @Test
  void verifySubjectThrowsProjectExceptionForInvalidToken() {
    when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("invalid"));

    assertThatThrownBy(() -> oidcIdTokenVerifier.verifySubject(jwtDecoder, "invalid-token"))
        .isInstanceOf(ProjectException.class);
  }

  @Test
  void verifySubjectThrowsProjectExceptionWhenTokenAlreadyUsed() {
    // 로그 유출 등으로 같은 ID 토큰이 재전송된 상황을 재현한다.
    Jwt jwt =
        Jwt.withTokenValue("replayed-token")
            .header("alg", "none")
            .claim("sub", "provider-user-id")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    when(jwtDecoder.decode("replayed-token")).thenReturn(jwt);
    when(redisTokenRepository.consumeOidcIdToken(eq("replayed-token"), any())).thenReturn(false);

    assertThatThrownBy(() -> oidcIdTokenVerifier.verifySubject(jwtDecoder, "replayed-token"))
        .isInstanceOf(ProjectException.class);
  }

  @Test
  void verifySubjectConsumesTokenWithTtlMatchingRemainingValidity() {
    Instant expiresAt = Instant.now().plusSeconds(120);
    Jwt jwt =
        Jwt.withTokenValue("id-token")
            .header("alg", "none")
            .claim("sub", "provider-user-id")
            .issuedAt(Instant.now())
            .expiresAt(expiresAt)
            .build();
    when(jwtDecoder.decode("id-token")).thenReturn(jwt);
    when(redisTokenRepository.consumeOidcIdToken(eq("id-token"), any())).thenReturn(true);

    oidcIdTokenVerifier.verifySubject(jwtDecoder, "id-token");

    verify(redisTokenRepository)
        .consumeOidcIdToken(
            eq("id-token"),
            org.mockito.ArgumentMatchers.argThat(
                ttl -> ttl.getSeconds() > 110 && ttl.getSeconds() <= 120));
  }

  @Test
  void verifySubjectThrowsProjectExceptionAndNeverCallsRedisWhenTokenAlreadyExpired() {
    // JwtValidators의 기본 만료 검증은 서버 간 시계 오차를 감안해 60초의 clock skew를
    // 허용하므로, 이미 실제로는 만료된 토큰도 decode()를 통과할 수 있다. 이런 토큰은
    // Redis 소모를 시도하면 안 된다 — 시도하면 음수/0짜리 TTL이 넘어간다.
    Jwt jwt =
        Jwt.withTokenValue("expired-token")
            .header("alg", "none")
            .claim("sub", "provider-user-id")
            .issuedAt(Instant.now().minusSeconds(120))
            .expiresAt(Instant.now().minusSeconds(30))
            .build();
    when(jwtDecoder.decode("expired-token")).thenReturn(jwt);

    assertThatThrownBy(() -> oidcIdTokenVerifier.verifySubject(jwtDecoder, "expired-token"))
        .isInstanceOf(ProjectException.class);

    verify(redisTokenRepository, never()).consumeOidcIdToken(any(), any());
  }

  @Test
  void verifySubjectThrowsProjectExceptionAndNeverCallsRedisWhenExpiresAtEqualsNow() {
    // 발급 시각(exp)이 현재 시각과 정확히 같은 경계값 — "아직 유효함"이 아니라 거부돼야 한다.
    Instant now = Instant.now();
    Jwt jwt =
        Jwt.withTokenValue("boundary-token")
            .header("alg", "none")
            .claim("sub", "provider-user-id")
            .issuedAt(now.minusSeconds(60))
            .expiresAt(now)
            .build();
    when(jwtDecoder.decode("boundary-token")).thenReturn(jwt);

    assertThatThrownBy(() -> oidcIdTokenVerifier.verifySubject(jwtDecoder, "boundary-token"))
        .isInstanceOf(ProjectException.class);

    verify(redisTokenRepository, never()).consumeOidcIdToken(any(), any());
  }
}
