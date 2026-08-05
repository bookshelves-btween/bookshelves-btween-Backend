package com.bookshelves.global.security;

import com.bookshelves.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final String MEMBER_ID_CLAIM = "memberId";
  private static final String TOKEN_TYPE_CLAIM = "tokenType";
  // JWT 표준 iat 클레임은 NumericDate(초 단위 정수)로 직렬화되어 밀리초 정보가 사라진다.
  // 로그아웃 시각(RedisTokenRepository.saveLogoutAt)은 밀리초 단위로 저장되므로, 정밀도가
  // 안 맞으면 로그아웃과 같은 초 안에서 재발급된 정상 토큰이 폐기된 토큰으로 오인될 수 있다.
  // 그래서 발급 시각을 별도 커스텀 클레임에 밀리초 그대로 싣는다.
  private static final String ISSUED_AT_MILLIS_CLAIM = "issuedAtMillis";

  private final JwtProperties jwtProperties;
  private final SecretKey secretKey;
  private final Clock clock;

  @Autowired
  public JwtTokenProvider(JwtProperties jwtProperties) {
    this(jwtProperties, Clock.systemUTC());
  }

  // 발급 시각을 밀리초 단위로 정밀 제어해 테스트하기 위한 생성자.
  JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
    this.jwtProperties = jwtProperties;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    this.clock = clock;
  }

  public String generateToken(Long memberId, TokenType tokenType) {
    Instant now = Instant.now(clock);
    Instant expiresAt = now.plusSeconds(getExpirationSeconds(tokenType));

    return Jwts.builder()
        .subject(String.valueOf(memberId))
        .id(UUID.randomUUID().toString())
        .claim(MEMBER_ID_CLAIM, memberId)
        .claim(TOKEN_TYPE_CLAIM, tokenType.name())
        .claim(ISSUED_AT_MILLIS_CLAIM, now.toEpochMilli())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public boolean isValidToken(String token, TokenType expectedTokenType) {
    try {
      Claims claims = parseClaims(token);
      // issuedAtMillis가 없는 토큰(이 클레임 도입 이전에 발급된 토큰 등)은 getIssuedAt()에서
      // NPE로 이어지므로 여기서 미리 걸러 인증 실패로 처리한다.
      return expectedTokenType.name().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
          && claims.get(ISSUED_AT_MILLIS_CLAIM, Long.class) != null;
    } catch (RuntimeException e) {
      return false;
    }
  }

  public Long getMemberId(String token) {
    return parseClaims(token).get(MEMBER_ID_CLAIM, Long.class);
  }

  public Instant getIssuedAt(String token) {
    return Instant.ofEpochMilli(parseClaims(token).get(ISSUED_AT_MILLIS_CLAIM, Long.class));
  }

  public long getExpirationSeconds(TokenType tokenType) {
    return switch (tokenType) {
      case ACCESS -> jwtProperties.accessTokenExpirationSeconds();
      case REFRESH -> jwtProperties.refreshTokenExpirationSeconds();
      case RESTORE -> jwtProperties.restoreTokenExpirationSeconds();
    };
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
