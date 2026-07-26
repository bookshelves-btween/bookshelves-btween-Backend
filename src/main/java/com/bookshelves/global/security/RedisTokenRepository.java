package com.bookshelves.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisTokenRepository {

  private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
  private static final String RESTORE_TOKEN_KEY_PREFIX = "auth:restore:";

  // 현재 저장된 값이 oldTokenHash와 일치할 때만 newTokenHash로 교체(CAS).
  // matches 확인과 rotation 사이에 다른 요청이 끼어들 틈을 Redis 레벨에서 원자적으로 차단한다.
  private static final RedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT =
      RedisScript.of(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then
            redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
            return 1
          else
            return 0
          end
          """,
          Long.class);

  // GET한 값이 tokenHash와 일치할 때만 원자적으로 삭제(소모)한다.
  // matches 확인과 delete 사이에 다른 요청이 끼어들어 같은 토큰으로 두 번 복구가
  // 성공하는 재사용(replay) 경쟁을 Redis 레벨에서 차단한다.
  private static final RedisScript<Long> CONSUME_RESTORE_TOKEN_SCRIPT =
      RedisScript.of(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then
            redis.call('DEL', KEYS[1])
            return 1
          else
            return 0
          end
          """,
          Long.class);

  private final StringRedisTemplate stringRedisTemplate;

  public RedisTokenRepository(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void saveRefreshToken(Long memberId, String refreshToken, Duration ttl) {
    saveToken(getRefreshTokenKey(memberId), refreshToken, ttl);
  }

  /** oldRefreshToken이 현재 저장된 값과 일치할 때만 newRefreshToken으로 원자적으로 교체한다. */
  public boolean rotateRefreshToken(
      Long memberId, String oldRefreshToken, String newRefreshToken, Duration ttl) {
    Long result =
        stringRedisTemplate.execute(
            ROTATE_REFRESH_TOKEN_SCRIPT,
            List.of(getRefreshTokenKey(memberId)),
            hash(oldRefreshToken),
            hash(newRefreshToken),
            String.valueOf(ttl.getSeconds()));

    return result != null && result == 1L;
  }

  public void deleteRefreshToken(Long memberId) {
    stringRedisTemplate.delete(getRefreshTokenKey(memberId));
  }

  public void saveRestoreToken(Long memberId, String restoreToken, Duration ttl) {
    saveToken(getRestoreTokenKey(memberId), restoreToken, ttl);
  }

  /** restoreToken이 현재 저장된 값과 일치할 때만 원자적으로 삭제(소모)한다. */
  public boolean consumeRestoreToken(Long memberId, String restoreToken) {
    Long result =
        stringRedisTemplate.execute(
            CONSUME_RESTORE_TOKEN_SCRIPT,
            List.of(getRestoreTokenKey(memberId)),
            hash(restoreToken));

    return result != null && result == 1L;
  }

  public void deleteAllTokens(Long memberId) {
    stringRedisTemplate.delete(getRefreshTokenKey(memberId));
    stringRedisTemplate.delete(getRestoreTokenKey(memberId));
  }

  private void saveToken(String key, String token, Duration ttl) {
    stringRedisTemplate.opsForValue().set(key, hash(token), ttl);
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
