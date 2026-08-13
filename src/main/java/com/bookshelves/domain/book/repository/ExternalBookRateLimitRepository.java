package com.bookshelves.domain.book.repository;

import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class ExternalBookRateLimitRepository {

  private static final String KEY_PREFIX = "book:rate-limit:v1:";
  private static final RedisScript<Long> INCREMENT_WITH_EXPIRATION_SCRIPT =
      RedisScript.of(
          """
          local count = redis.call('INCR', KEYS[1])
          if count == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])
          end
          return count
          """,
          Long.class);

  private final StringRedisTemplate stringRedisTemplate;

  public ExternalBookRateLimitRepository(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public long increment(Long memberId, String requestType, Duration window) {
    Long count =
        stringRedisTemplate.execute(
            INCREMENT_WITH_EXPIRATION_SCRIPT,
            List.of(rateLimitKey(memberId, requestType)),
            String.valueOf(window.toSeconds()));
    if (count == null) {
      throw new IllegalStateException("외부 도서 요청 횟수를 확인할 수 없습니다.");
    }
    return count;
  }

  private String rateLimitKey(Long memberId, String requestType) {
    return KEY_PREFIX + requestType + ":member:" + memberId;
  }
}
