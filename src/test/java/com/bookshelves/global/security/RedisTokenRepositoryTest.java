package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings("unchecked")
class RedisTokenRepositoryTest {

  private final StringRedisTemplate stringRedisTemplate =
      org.mockito.Mockito.mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> valueOperations =
      org.mockito.Mockito.mock(ValueOperations.class);
  private final RedisTokenRepository redisTokenRepository =
      new RedisTokenRepository(stringRedisTemplate);

  @Test
  void saveRefreshTokenStoresHashedToken() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

    redisTokenRepository.saveRefreshToken(1L, "refresh-token", Duration.ofSeconds(60));

    verify(valueOperations)
        .set(
            org.mockito.Mockito.eq("auth:refresh:1"),
            valueCaptor.capture(),
            org.mockito.Mockito.eq(Duration.ofSeconds(60)));
    assertThat(valueCaptor.getValue()).isNotEqualTo("refresh-token");
    assertThat(valueCaptor.getValue()).hasSize(64);
  }

  @Test
  @SuppressWarnings("unchecked")
  void rotateRefreshTokenReturnsTrueWhenScriptReportsMatch() {
    when(stringRedisTemplate.execute(
            any(RedisScript.class),
            eq(List.of("auth:refresh:1", "auth:refresh:grace:1")),
            any(),
            any(),
            any(),
            any(),
            any()))
        .thenReturn(1L);

    boolean rotated =
        redisTokenRepository.rotateRefreshToken(
            1L, "old-token", "new-token", Duration.ofSeconds(60), "grace-payload");

    assertThat(rotated).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void rotateRefreshTokenReturnsFalseWhenScriptReportsMismatch() {
    when(stringRedisTemplate.execute(
            any(RedisScript.class),
            eq(List.of("auth:refresh:1", "auth:refresh:grace:1")),
            any(),
            any(),
            any(),
            any(),
            any()))
        .thenReturn(0L);

    boolean rotated =
        redisTokenRepository.rotateRefreshToken(
            1L, "old-token", "new-token", Duration.ofSeconds(60), "grace-payload");

    assertThat(rotated).isFalse();
  }

  @Test
  void findRotationGracePayloadReturnsPayloadWhenOldTokenMatchesGraceRecord() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    // rotateRefreshToken이 실제로 저장하는 형식(oldTokenHash|payload)을 그대로 흉내낸다.
    when(valueOperations.get("auth:refresh:grace:1"))
        .thenReturn(sha256("old-token") + "|new-access|new-refresh|3600|1209600");

    Optional<String> payload = redisTokenRepository.findRotationGracePayload(1L, "old-token");

    assertThat(payload).contains("new-access|new-refresh|3600|1209600");
  }

  @Test
  void findRotationGracePayloadReturnsEmptyWhenNoGraceRecordExists() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("auth:refresh:grace:1")).thenReturn(null);

    Optional<String> payload = redisTokenRepository.findRotationGracePayload(1L, "old-token");

    assertThat(payload).isEmpty();
  }

  @Test
  void findRotationGracePayloadReturnsEmptyWhenOldTokenDoesNotMatchGraceRecord() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("auth:refresh:grace:1"))
        .thenReturn("some-other-token-hash|new-access|new-refresh|3600|1209600");

    Optional<String> payload = redisTokenRepository.findRotationGracePayload(1L, "old-token");

    assertThat(payload).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void consumeRestoreTokenReturnsTrueWhenScriptReportsMatch() {
    when(stringRedisTemplate.execute(any(RedisScript.class), eq(List.of("auth:restore:1")), any()))
        .thenReturn(1L);

    boolean consumed = redisTokenRepository.consumeRestoreToken(1L, "restore-token");

    assertThat(consumed).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void consumeRestoreTokenReturnsFalseWhenScriptReportsMismatch() {
    when(stringRedisTemplate.execute(any(RedisScript.class), eq(List.of("auth:restore:1")), any()))
        .thenReturn(0L);

    boolean consumed = redisTokenRepository.consumeRestoreToken(1L, "restore-token");

    assertThat(consumed).isFalse();
  }

  @Test
  void deleteAllTokensDeletesRefreshRestoreAndGraceTokens() {
    redisTokenRepository.deleteAllTokens(1L);

    verify(stringRedisTemplate).delete("auth:refresh:1");
    verify(stringRedisTemplate).delete("auth:restore:1");
    verify(stringRedisTemplate).delete("auth:refresh:grace:1");
  }

  private String sha256(String value) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder();
      for (byte b : digest) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
