package com.bookshelves.domain.book.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class ExternalBookRateLimitRepositoryTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @InjectMocks private ExternalBookRateLimitRepository externalBookRateLimitRepository;

  @Test
  void incrementUsesVersionedMemberSearchKeyAndSixtySecondWindow() {
    given(
            stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("book:rate-limit:v1:search:member:7")),
                eq("60")))
        .willReturn(1L);

    long count = externalBookRateLimitRepository.increment(7L, "search", Duration.ofMinutes(1));

    assertThat(count).isEqualTo(1L);
    verify(stringRedisTemplate)
        .execute(
            any(RedisScript.class), eq(List.of("book:rate-limit:v1:search:member:7")), eq("60"));
  }

  @Test
  void incrementSeparatesDetailKeyByMember() {
    given(
            stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("book:rate-limit:v1:detail:member:8")),
                eq("60")))
        .willReturn(30L);

    long count = externalBookRateLimitRepository.increment(8L, "detail", Duration.ofMinutes(1));

    assertThat(count).isEqualTo(30L);
  }

  @Test
  void incrementThrowsWhenRedisReturnsNoCount() {
    given(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), eq("60")))
        .willReturn(null);

    assertThatThrownBy(
            () -> externalBookRateLimitRepository.increment(7L, "search", Duration.ofMinutes(1)))
        .isInstanceOf(IllegalStateException.class);
  }
}
