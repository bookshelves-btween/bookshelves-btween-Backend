package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.ExternalBookRateLimitRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExternalBookRateLimitService {

  private static final long FAILURE_LOG_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();

  private final ExternalBookRateLimitRepository externalBookRateLimitRepository;
  private final AuthenticationFacade authenticationFacade;
  private final int requestLimit;
  private final Duration window;
  private final Map<RequestType, Counter> redisFailureCounters;
  private final AtomicLong nextFailureLogAtNanos = new AtomicLong();

  public ExternalBookRateLimitService(
      ExternalBookRateLimitRepository externalBookRateLimitRepository,
      AuthenticationFacade authenticationFacade,
      MeterRegistry meterRegistry,
      @Value("${book.rate-limit.request-limit:30}") int requestLimit,
      @Value("${book.rate-limit.window:1m}") Duration window) {
    if (requestLimit < 1) {
      throw new IllegalArgumentException("외부 도서 요청 제한 횟수는 1 이상이어야 합니다.");
    }
    if (window == null || window.toSeconds() < 1) {
      throw new IllegalArgumentException("외부 도서 요청 제한 시간은 1초 이상이어야 합니다.");
    }
    this.externalBookRateLimitRepository = externalBookRateLimitRepository;
    this.authenticationFacade = authenticationFacade;
    this.requestLimit = requestLimit;
    this.window = window;
    this.redisFailureCounters = createRedisFailureCounters(meterRegistry);
  }

  public void check(RequestType requestType) {
    Long memberId = authenticationFacade.getCurrentMemberId();
    long requestCount;
    try {
      requestCount = externalBookRateLimitRepository.increment(memberId, requestType.key(), window);
    } catch (RuntimeException ignored) {
      redisFailureCounters.get(requestType).increment();
      logRedisFailure(requestType);
      return;
    }

    if (requestCount > requestLimit) {
      throw new BookException(BookErrorCode.EXTERNAL_BOOK_RATE_LIMIT_EXCEEDED);
    }
  }

  private Map<RequestType, Counter> createRedisFailureCounters(MeterRegistry meterRegistry) {
    Map<RequestType, Counter> counters = new EnumMap<>(RequestType.class);
    for (RequestType requestType : RequestType.values()) {
      counters.put(
          requestType,
          Counter.builder("book.rate_limit.redis.failures")
              .description("외부 도서 레이트 리밋 Redis 처리 실패 횟수")
              .tag("request.type", requestType.key())
              .register(meterRegistry));
    }
    return Map.copyOf(counters);
  }

  private void logRedisFailure(RequestType requestType) {
    long now = System.nanoTime();
    long nextLogAt = nextFailureLogAtNanos.get();
    if (now >= nextLogAt
        && nextFailureLogAtNanos.compareAndSet(nextLogAt, now + FAILURE_LOG_INTERVAL_NANOS)) {
      log.warn("외부 도서 요청 제한 Redis 처리에 실패해 요청을 허용합니다. requestType={}", requestType);
    }
  }

  public enum RequestType {
    SEARCH("search"),
    DETAIL("detail");

    private final String key;

    RequestType(String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }
  }
}
