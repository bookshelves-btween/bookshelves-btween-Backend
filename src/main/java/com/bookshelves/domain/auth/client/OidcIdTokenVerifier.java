package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.global.security.RedisTokenRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class OidcIdTokenVerifier {

  private final RedisTokenRepository redisTokenRepository;

  public OidcIdTokenVerifier(RedisTokenRepository redisTokenRepository) {
    this.redisTokenRepository = redisTokenRepository;
  }

  public String verifySubject(JwtDecoder jwtDecoder, String idToken) {
    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(idToken);
    } catch (JwtException e) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }

    // ID 토큰은 서명이 유효하고 만료 전이면 몇 번이든 재검증을 통과한다. 로그·크래시
    // 리포트 등으로 토큰이 유출됐을 때의 재사용을 막기 위해, 만료까지 남은 시간만큼
    // Redis에 1회용으로 소모한다. jti 클레임은 구글/애플 ID 토큰에 항상 있다고 보장되지
    // 않아, 토큰 원문 자체를 식별자로 쓴다.
    //
    // JwtValidators.createDefaultWithIssuer의 기본 만료 검증은 서버 간 시계 오차를 감안해
    // 60초의 clock skew를 허용하므로, decode()를 통과했다고 해서 진짜로 아직 안 만료된
    // 것은 아니다. Redis에 음수/0짜리 TTL을 넘기지 않도록, 우리 쪽에서 별도로 다시
    // 만료 여부를 확인한다.
    Instant expiresAt = jwt.getExpiresAt();
    Instant now = Instant.now();
    if (expiresAt == null || !expiresAt.isAfter(now)) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }

    if (!redisTokenRepository.consumeOidcIdToken(idToken, Duration.between(now, expiresAt))) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }

    return jwt.getSubject();
  }
}
