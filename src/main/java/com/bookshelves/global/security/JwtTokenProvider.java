package com.bookshelves.global.security;

import com.bookshelves.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final String MEMBER_ID_CLAIM = "memberId";
  private static final String TOKEN_TYPE_CLAIM = "tokenType";

  private final JwtProperties jwtProperties;
  private final SecretKey secretKey;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(Long memberId, TokenType tokenType) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(getExpirationSeconds(tokenType));

    return Jwts.builder()
        .subject(String.valueOf(memberId))
        .claim(MEMBER_ID_CLAIM, memberId)
        .claim(TOKEN_TYPE_CLAIM, tokenType.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  public boolean isValidToken(String token, TokenType expectedTokenType) {
    try {
      Claims claims = parseClaims(token);
      return expectedTokenType.name().equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    } catch (RuntimeException e) {
      return false;
    }
  }

  public Long getMemberId(String token) {
    return parseClaims(token).get(MEMBER_ID_CLAIM, Long.class);
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
