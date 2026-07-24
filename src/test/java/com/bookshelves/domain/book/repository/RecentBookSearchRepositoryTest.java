package com.bookshelves.domain.book.repository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RecentBookSearchRepositoryTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @InjectMocks private RecentBookSearchRepository recentBookSearchRepository;

  @Test
  void saveUsesMemberKeyMaximumFiveAndThirtyDayTtl() {
    recentBookSearchRepository.save(7L, "혼모노");

    verify(stringRedisTemplate)
        .execute(
            org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
            eq(List.of("recent-searches:member:7")),
            anyString(),
            eq("혼모노"),
            eq("5"),
            eq("2592000"));
  }
}
