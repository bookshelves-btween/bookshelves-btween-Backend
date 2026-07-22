package com.bookshelves.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisTokenRepository {

  private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
  private static final String RESTORE_TOKEN_KEY_PREFIX = "auth:restore:";

  private final StringRedisTemplate stringRedisTemplate;

  public RedisTokenRepository(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void saveRefreshToken(Long memberId, String refreshToken, Duration ttl) {
    saveToken(getRefreshTokenKey(memberId), refreshToken, ttl);
  }

  public boolean matchesRefreshToken(Long memberId, String refreshToken) {
    return matchesToken(getRefreshTokenKey(memberId), refreshToken);
  }

  public void deleteRefreshToken(Long memberId) {
    stringRedisTemplate.delete(getRefreshTokenKey(memberId));
  }

  public void saveRestoreToken(Long memberId, String restoreToken, Duration ttl) {
    saveToken(getRestoreTokenKey(memberId), restoreToken, ttl);
  }

  public boolean matchesRestoreToken(Long memberId, String restoreToken) {
    return matchesToken(getRestoreTokenKey(memberId), restoreToken);
  }

  public void deleteRestoreToken(Long memberId) {
    stringRedisTemplate.delete(getRestoreTokenKey(memberId));
  }

  public void deleteAllTokens(Long memberId) {
    stringRedisTemplate.delete(getRefreshTokenKey(memberId));
    stringRedisTemplate.delete(getRestoreTokenKey(memberId));
  }

  private void saveToken(String key, String token, Duration ttl) {
    stringRedisTemplate.opsForValue().set(key, hash(token), ttl);
  }

  private boolean matchesToken(String key, String token) {
    String savedTokenHash = stringRedisTemplate.opsForValue().get(key);
    String tokenHash = hash(token);

    return savedTokenHash != null
        && MessageDigest.isEqual(
            savedTokenHash.getBytes(StandardCharsets.UTF_8),
            tokenHash.getBytes(StandardCharsets.UTF_8));
  }

  private String getRefreshTokenKey(Long memberId) {
    return REFRESH_TOKEN_KEY_PREFIX + memberId;
  }

  private String getRestoreTokenKey(Long memberId) {
    return RESTORE_TOKEN_KEY_PREFIX + memberId;
  }

  private String hash(String token) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] digest = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();

      for (byte b : digest) {
        builder.append(String.format("%02x", b));
      }

      return builder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", e);
    }
  }
}
