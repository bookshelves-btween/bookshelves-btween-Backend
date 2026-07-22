package com.bookshelves.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
  void matchesRefreshTokenComparesStoredHash() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    redisTokenRepository.saveRefreshToken(1L, "refresh-token", Duration.ofSeconds(60));
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    verify(valueOperations)
        .set(
            org.mockito.Mockito.eq("auth:refresh:1"),
            valueCaptor.capture(),
            org.mockito.Mockito.eq(Duration.ofSeconds(60)));
    when(valueOperations.get("auth:refresh:1")).thenReturn(valueCaptor.getValue());

    assertThat(redisTokenRepository.matchesRefreshToken(1L, "refresh-token")).isTrue();
    assertThat(redisTokenRepository.matchesRefreshToken(1L, "other-token")).isFalse();
  }

  @Test
  void deleteAllTokensDeletesRefreshAndRestoreTokens() {
    redisTokenRepository.deleteAllTokens(1L);

    verify(stringRedisTemplate).delete("auth:refresh:1");
    verify(stringRedisTemplate).delete("auth:restore:1");
  }
}
