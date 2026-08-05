package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.global.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final String SECRET = "bookshelves-test-jwt-secret-key-value";

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(new JwtProperties(SECRET, 3600, 1209600, 600));

  @Test
  void generateAndValidateAccessToken() {
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);

    assertThat(jwtTokenProvider.isValidToken(token, TokenType.ACCESS)).isTrue();
    assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(1L);
  }

  @Test
  void tokenTypeMismatchIsInvalid() {
    String token = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);

    assertThat(jwtTokenProvider.isValidToken(token, TokenType.ACCESS)).isFalse();
  }

  @Test
  void invalidTokenIsInvalid() {
    assertThat(jwtTokenProvider.isValidToken("invalid-token", TokenType.ACCESS)).isFalse();
  }

  @Test
  void tokenMissingIssuedAtMillisClaimIsInvalid() {
    // issuedAtMillis 클레임 도입 이전에 발급된, 서명은 유효한 토큰을 흉내낸다.
    // 이 클레임이 없으면 getIssuedAt()에서 NPE가 나므로 isValidToken 단계에서 걸러야 한다.
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

    assertThat(jwtTokenProvider.isValidToken(legacyToken, TokenType.ACCESS)).isFalse();
  }

  @Test
  void expirationSecondsFollowTokenType() {
    assertThat(jwtTokenProvider.getExpirationSeconds(TokenType.ACCESS)).isEqualTo(3600);
    assertThat(jwtTokenProvider.getExpirationSeconds(TokenType.REFRESH)).isEqualTo(1209600);
    assertThat(jwtTokenProvider.getExpirationSeconds(TokenType.RESTORE)).isEqualTo(600);
  }

  @Test
  void getIssuedAtReturnsTimeCloseToGeneration() {
    Instant before = Instant.now();
    String token = jwtTokenProvider.generateToken(1L, TokenType.ACCESS);
    Instant after = Instant.now();

    Instant issuedAt = jwtTokenProvider.getIssuedAt(token);

    assertThat(issuedAt).isBetween(before.minusSeconds(1), after.plusSeconds(1));
  }

  @Test
  void tokensGeneratedForSameMemberAndTypeAreAlwaysUnique() {
    String first = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    String second = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);

    assertThat(first).isNotEqualTo(second);
  }
}
