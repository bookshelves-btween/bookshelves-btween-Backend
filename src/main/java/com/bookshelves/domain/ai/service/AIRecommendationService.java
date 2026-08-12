package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.client.GeminiRecommendationClient;
import com.bookshelves.domain.ai.entity.AIRecommendation;
import com.bookshelves.domain.ai.repository.AIRecommendationRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

// 문학과 철학 도서 중 하루 한 권의 추천을 준비한다.
// LLM 호출 중 커넥션을 점유하지 않도록 전체 작업을 트랜잭션으로 묶지 않는다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRecommendationService {

  // 최근 추천을 제외하되 후보가 없으면 전체 대상에서 선택한다.
  static final int EXCLUSION_WINDOW_DAYS = 10;

  // 책 소개도 없을 때 사용하는 기본 문구.
  static final String DEFAULT_MESSAGE = "오늘의 책으로 골라봤어요";

  private static final int FALLBACK_MAX_LENGTH = 126;
  private static final String TRUNCATION_SUFFIX = "...";

  // 숫자 사이의 마침표를 제외한 첫 문장 경계를 찾는다.
  private static final Pattern FIRST_SENTENCE =
      Pattern.compile("^.*?[.!?](?=\\s|$)", Pattern.DOTALL);

  private final BookRepository bookRepository;
  private final AIRecommendationRepository aiRecommendationRepository;
  private final GeminiRecommendationClient geminiRecommendationClient;

  /** 해당 날짜의 추천이 이미 있으면 건너뛴다. */
  public void prepare(LocalDate recommendedDate) {
    if (aiRecommendationRepository.existsByRecommendedDate(recommendedDate)) {
      return;
    }

    Long bookId = pickBookId(recommendedDate);
    if (bookId == null) {
      log.warn("추천 대상 책이 없어 오늘의 추천을 건너뛴다: recommendedDate={}", recommendedDate);
      return;
    }

    Book book = bookRepository.findById(bookId).orElse(null);
    if (book == null) {
      log.warn("추천 후보 책이 사라져 건너뛴다: bookId={}", bookId);
      return;
    }

    save(book, buildMessage(book), recommendedDate);
  }

  private Long pickBookId(LocalDate recommendedDate) {
    List<Long> recommendableIds = bookRepository.findRecommendableIds();
    if (recommendableIds.isEmpty()) {
      return null;
    }

    Set<Long> recentlyUsed =
        new HashSet<>(
            aiRecommendationRepository.findBookIdsRecommendedSince(
                recommendedDate.minusDays(EXCLUSION_WINDOW_DAYS)));

    List<Long> candidates =
        recommendableIds.stream().filter(id -> !recentlyUsed.contains(id)).toList();
    if (candidates.isEmpty()) {
      log.info("최근 추천을 제외하면 후보가 없어 전체에서 고른다: 전체={}", recommendableIds.size());
      candidates = recommendableIds;
    }
    return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
  }

  private String buildMessage(Book book) {
    try {
      String generated = geminiRecommendationClient.generateMessage(book);
      if (generated != null) {
        return generated;
      }
    } catch (Exception e) {
      log.error("추천 멘트 생성에 실패해 책 소개로 폴백한다: bookId={}", book.getId(), e);
    }
    return fallbackMessage(book);
  }

  private String fallbackMessage(Book book) {
    String description = book.getDescription();
    if (description == null || description.isBlank()) {
      return DEFAULT_MESSAGE;
    }

    String normalized = description.strip().replaceAll("\\s+", " ");
    Matcher matcher = FIRST_SENTENCE.matcher(normalized);
    String sentence = matcher.find() ? matcher.group() : normalized;

    return sentence.length() <= FALLBACK_MAX_LENGTH
        ? sentence
        : sentence.substring(0, FALLBACK_MAX_LENGTH) + TRUNCATION_SUFFIX;
  }

  // 단일 INSERT는 리포지토리 트랜잭션에 맡긴다.
  private void save(Book book, String message, LocalDate recommendedDate) {
    try {
      aiRecommendationRepository.save(
          AIRecommendation.builder()
              .book(book)
              .recommendationMessage(message)
              .recommendedDate(recommendedDate)
              .build());
    } catch (DataIntegrityViolationException e) {
      // 동시 실행의 중복 저장은 날짜 unique 제약으로 막는다.
      log.info("같은 날짜의 추천이 이미 저장돼 건너뛴다: recommendedDate={}", recommendedDate);
    }
  }
}
