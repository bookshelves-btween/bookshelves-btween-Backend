package com.bookshelves.domain.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.ExternalBookRateLimitRepository;
import com.bookshelves.domain.book.service.ExternalBookRateLimitService.RequestType;
import com.bookshelves.global.security.AuthenticationFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalBookRateLimitServiceTest {

  @Mock private ExternalBookRateLimitRepository externalBookRateLimitRepository;
  @Mock private AuthenticationFacade authenticationFacade;

  private ExternalBookRateLimitService externalBookRateLimitService;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    externalBookRateLimitService =
        new ExternalBookRateLimitService(
            externalBookRateLimitRepository,
            authenticationFacade,
            meterRegistry,
            30,
            Duration.ofMinutes(1));
    lenient().when(authenticationFacade.getCurrentMemberId()).thenReturn(7L);
  }

  @Test
  void allowsRequestAtConfiguredLimit() {
    given(externalBookRateLimitRepository.increment(7L, "search", Duration.ofMinutes(1)))
        .willReturn(30L);

    assertThatCode(() -> externalBookRateLimitService.check(RequestType.SEARCH))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsRequestOverConfiguredLimit() {
    given(externalBookRateLimitRepository.increment(7L, "detail", Duration.ofMinutes(1)))
        .willReturn(31L);

    assertThatThrownBy(() -> externalBookRateLimitService.check(RequestType.DETAIL))
        .isInstanceOf(BookException.class)
        .extracting(exception -> ((BookException) exception).getErrorCode())
        .isEqualTo(BookErrorCode.EXTERNAL_BOOK_RATE_LIMIT_EXCEEDED);
  }

  @Test
  void redisFailureAllowsRequest() {
    given(externalBookRateLimitRepository.increment(7L, "search", Duration.ofMinutes(1)))
        .willThrow(new RuntimeException("redis unavailable"));

    assertThatCode(() -> externalBookRateLimitService.check(RequestType.SEARCH))
        .doesNotThrowAnyException();
    assertThat(
            meterRegistry
                .get("book.rate_limit.redis.failures")
                .tag("request.type", "search")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void rejectsNonPositiveRequestLimitAtStartup() {
    assertThatThrownBy(
            () ->
                new ExternalBookRateLimitService(
                    externalBookRateLimitRepository,
                    authenticationFacade,
                    new SimpleMeterRegistry(),
                    0,
                    Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsSubSecondWindowAtStartup() {
    assertThatThrownBy(
            () ->
                new ExternalBookRateLimitService(
                    externalBookRateLimitRepository,
                    authenticationFacade,
                    new SimpleMeterRegistry(),
                    30,
                    Duration.ofMillis(999)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
