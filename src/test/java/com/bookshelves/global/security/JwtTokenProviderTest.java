package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookshelves.global.config.JwtProperties;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(
          new JwtProperties("bookshelves-test-jwt-secret-key-value", 3600, 1209600, 600));

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
  void expirationSecondsFollowTokenType() {
    assertThat(jwtTokenProvider.getExpirationSeconds(TokenType.ACCESS)).isEqualTo(3600);
    assertThat(jwtTokenProvider.getExpirationSeconds(TokenType.REFRESH)).isEqualTo(1209600);
    assertThat(jwtTokenProvider.getExpirationSeconds(TokenType.RESTORE)).isEqualTo(600);
  }
}
