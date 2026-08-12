package com.bookshelves.domain.book.service;

import com.bookshelves.domain.book.exception.BookException;
import com.bookshelves.domain.book.exception.code.BookErrorCode;
import com.bookshelves.domain.book.repository.ExternalBookRateLimitRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExternalBookRateLimitService {

  private final ExternalBookRateLimitRepository externalBookRateLimitRepository;
  private final AuthenticationFacade authenticationFacade;
  private final int requestLimit;
  private final Duration window;

  public ExternalBookRateLimitService(
      ExternalBookRateLimitRepository externalBookRateLimitRepository,
      AuthenticationFacade authenticationFacade,
      @Value("${book.rate-limit.request-limit:30}") int requestLimit,
      @Value("${book.rate-limit.window:1m}") Duration window) {
    this.externalBookRateLimitRepository = externalBookRateLimitRepository;
    this.authenticationFacade = authenticationFacade;
    this.requestLimit = requestLimit;
    this.window = window;
  }

  public void check(RequestType requestType) {
    Long memberId = authenticationFacade.getCurrentMemberId();
    long requestCount;
    try {
      requestCount =
          externalBookRateLimitRepository.increment(memberId, requestType.key(), window);
    } catch (RuntimeException exception) {
      log.warn(
          "외부 도서 요청 제한을 확인하지 못해 요청을 허용합니다. memberId={}, requestType={}",
          memberId,
          requestType,
          exception);
      return;
    }

    if (requestCount > requestLimit) {
      throw new BookException(BookErrorCode.EXTERNAL_BOOK_RATE_LIMIT_EXCEEDED);
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
