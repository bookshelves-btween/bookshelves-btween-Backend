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
    Instant expiresAt = jwt.getExpiresAt();
    if (expiresAt == null
        || !redisTokenRepository.consumeOidcIdToken(
            idToken, Duration.between(Instant.now(), expiresAt))) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }

    return jwt.getSubject();
  }
}
