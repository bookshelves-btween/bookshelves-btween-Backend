package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.global.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
  void getIssuedAtReturnsExactGenerationInstantWithMillisecondPrecision() {
    // 넓은 허용 범위(예: ±1초)로 검증하면 iat이 초 단위로 잘리는 회귀가 나도 범위 안에 들어와
    // 못 잡는다. 밀리초가 0이 아닌 시각을 고정해 정확히 일치하는지 확인해야 그 회귀를 잡는다.
    Instant fixedInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(123);
    JwtTokenProvider fixedClockTokenProvider =
        new JwtTokenProvider(
            new JwtProperties(SECRET, 3600, 1209600, 600),
            Clock.fixed(fixedInstant, ZoneOffset.UTC));

    String token = fixedClockTokenProvider.generateToken(1L, TokenType.ACCESS);

    assertThat(jwtTokenProvider.getIssuedAt(token)).isEqualTo(fixedInstant);
  }

  @Test
  void tokensGeneratedForSameMemberAndTypeAreAlwaysUnique() {
    String first = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);
    String second = jwtTokenProvider.generateToken(1L, TokenType.REFRESH);

    assertThat(first).isNotEqualTo(second);
  }
}
