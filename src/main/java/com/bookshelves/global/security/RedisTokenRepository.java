package com.bookshelves.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisTokenRepository {

  private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";
  private static final String RESTORE_TOKEN_KEY_PREFIX = "auth:restore:";
  private static final String GRACE_TOKEN_KEY_PREFIX = "auth:refresh:grace:";
  private static final String LOGOUT_AT_KEY_PREFIX = "auth:logout-at:";
  private static final String OIDC_ID_TOKEN_KEY_PREFIX = "auth:oidc-used:";

  // accessToken 만료 순간 같은 refreshToken으로 동시에 여러 요청이 들어오면 CAS엔 하나만
  // 성공하는데, 그 요청이 발급한 토큰을 grace 키에 짧게 남겨 나머지("진") 요청도 같은
  // 토큰을 돌려받게 한다. 자세한 배경은 rotateRefreshToken()의 주석 참고.
  private static final Duration ROTATION_GRACE_TTL = Duration.ofSeconds(8);

  // 현재 저장된 값이 oldTokenHash와 일치할 때만 newTokenHash로 교체(CAS)하고, 같은 원자적
  // 실행 안에서 구 토큰 → gracePayload 매핑을 grace 키에 남긴다.
  // matches 확인과 rotation 사이에 다른 요청이 끼어들 틈을 Redis 레벨에서 원자적으로 차단한다.
  private static final RedisScript<Long> ROTATE_REFRESH_TOKEN_SCRIPT =
      RedisScript.of(
          """
          if redis.call('GET', KEYS[1]) == ARGV[1] then
            redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
            redis.call('SET', KEYS[2], ARGV[1] .. '|' .. ARGV[4], 'EX', ARGV[5])
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

  /**
   * oldRefreshToken이 현재 저장된 값과 일치할 때만 newRefreshToken으로 원자적으로 교체하고, gracePayload를 구 토큰과 묶어 짧게
   * 저장해둔다. 이 토큰으로 동시에 들어온 다른 요청은 findRotationGracePayload()로 이 값을 그대로 돌려받아 불필요하게 실패하지 않는다.
   */
  public boolean rotateRefreshToken(
      Long memberId,
      String oldRefreshToken,
      String newRefreshToken,
      Duration ttl,
      String gracePayload) {
    Long result =
        stringRedisTemplate.execute(
            ROTATE_REFRESH_TOKEN_SCRIPT,
            List.of(getRefreshTokenKey(memberId), getGraceKey(memberId)),
            hash(oldRefreshToken),
            hash(newRefreshToken),
            String.valueOf(ttl.getSeconds()),
            gracePayload,
            String.valueOf(ROTATION_GRACE_TTL.getSeconds()));

    return result != null && result == 1L;
  }

  /**
   * CAS 회전에서 진 요청을 위한 조회. oldRefreshToken이 방금 회전에 성공한 요청과 같은 토큰이었다면, 그 요청이 저장해둔 gracePayload를 그대로
   * 돌려준다.
   */
  public Optional<String> findRotationGracePayload(Long memberId, String oldRefreshToken) {
    String grace = stringRedisTemplate.opsForValue().get(getGraceKey(memberId));
    if (grace == null) {
      return Optional.empty();
    }

    int separatorIndex = grace.indexOf('|');
    if (separatorIndex < 0 || !grace.substring(0, separatorIndex).equals(hash(oldRefreshToken))) {
      return Optional.empty();
    }

    return Optional.of(grace.substring(separatorIndex + 1));
  }

  /**
   * refreshToken과 그에 딸린 grace 키를 함께 원자적으로 삭제한다. grace 키를 같이 지우지 않으면, 로그아웃 직후에도 grace 유예 시간(최대 8초)
   * 동안 방금 무효화한 refreshToken으로 새 토큰 쌍을 발급받을 수 있다.
   */
  public void deleteRefreshToken(Long memberId) {
    stringRedisTemplate.delete(List.of(getRefreshTokenKey(memberId), getGraceKey(memberId)));
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

  /**
   * 로그아웃 시각을 accessToken 최대 수명만큼의 TTL로 저장한다. JwtAuthenticationFilter가 이 시각보다 먼저 발급된 accessToken을
   * 걸러내는 데 쓴다. TTL이 지나면 그 시점 이전에 발급된 토큰은 어차피 자연 만료되므로 더 기억할 필요가 없다.
   */
  public void saveLogoutAt(Long memberId, Duration accessTokenTtl) {
    stringRedisTemplate
        .opsForValue()
        .set(
            getLogoutAtKey(memberId), String.valueOf(Instant.now().toEpochMilli()), accessTokenTtl);
  }

  public Optional<Instant> findLogoutAt(Long memberId) {
    String value = stringRedisTemplate.opsForValue().get(getLogoutAtKey(memberId));
    if (value == null) {
      return Optional.empty();
    }

    return Optional.of(Instant.ofEpochMilli(Long.parseLong(value)));
  }

  /**
   * OIDC ID 토큰(구글/애플)을 1회용으로 소모한다. 이미 소모된 토큰이면 false를 반환한다. ID 토큰은 서명이 유효하고 만료 전이면 몇 번이든 재검증을
   * 통과하므로, 로그·크래시 리포트 등으로 유출됐을 때의 재사용을 막기 위해 만료까지 남은 시간만큼만 기록해둔다. SET NX가 원자적이라 동시에 같은 토큰이 두 번 들어와도
   * 하나만 성공한다.
   */
  public boolean consumeOidcIdToken(String idToken, Duration ttl) {
    Boolean result =
        stringRedisTemplate.opsForValue().setIfAbsent(getOidcIdTokenKey(idToken), "1", ttl);
    return Boolean.TRUE.equals(result);
  }

  public void deleteAllTokens(Long memberId) {
    stringRedisTemplate.delete(
        List.of(getRefreshTokenKey(memberId), getRestoreTokenKey(memberId), getGraceKey(memberId)));
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

  private String getGraceKey(Long memberId) {
    return GRACE_TOKEN_KEY_PREFIX + memberId;
  }

  private String getLogoutAtKey(Long memberId) {
    return LOGOUT_AT_KEY_PREFIX + memberId;
  }

  private String getOidcIdTokenKey(String idToken) {
    return OIDC_ID_TOKEN_KEY_PREFIX + hash(idToken);
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
