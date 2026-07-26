package com.bookshelves.domain.book.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RecentBookSearchRepository {

  private static final String KEY_PREFIX = "recent-searches:member:";
  private static final int MAX_SEARCH_COUNT = 5;
  private static final Duration TTL = Duration.ofDays(30);

  private static final RedisScript<Long> SAVE_RECENT_SEARCH_SCRIPT =
      RedisScript.of(
          """
          redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
          local count = redis.call('ZCARD', KEYS[1])
          local maxCount = tonumber(ARGV[3])
          if count > maxCount then
            redis.call('ZREMRANGEBYRANK', KEYS[1], 0, count - maxCount - 1)
          end
          redis.call('EXPIRE', KEYS[1], ARGV[4])
          return 1
          """,
          Long.class);

  private final StringRedisTemplate stringRedisTemplate;

  public RecentBookSearchRepository(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public void save(Long memberId, String query) {
    stringRedisTemplate.execute(
        SAVE_RECENT_SEARCH_SCRIPT,
        List.of(KEY_PREFIX + memberId),
        String.valueOf(Instant.now().toEpochMilli()),
        query,
        String.valueOf(MAX_SEARCH_COUNT),
        String.valueOf(TTL.toSeconds()));
  }

  public List<RecentSearch> findAllByMemberId(Long memberId) {
    Set<TypedTuple<String>> tuples =
        stringRedisTemplate
            .opsForZSet()
            .reverseRangeWithScores(KEY_PREFIX + memberId, 0, MAX_SEARCH_COUNT - 1);

    if (tuples == null || tuples.isEmpty()) {
      return List.of();
    }

    return tuples.stream()
        .filter(tuple -> tuple.getValue() != null && tuple.getScore() != null)
        .map(tuple -> new RecentSearch(tuple.getValue(), tuple.getScore().longValue()))
        .toList();
  }

  public record RecentSearch(String keyword, long searchedAtEpochMillis) {}
}
