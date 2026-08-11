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

// 오늘의 추천 도서를 하루 한 권 준비한다. 후보는 문학(KDC 800)과 철학(KDC 100)으로 분류된 책뿐이다.
//
// 이 클래스에는 트랜잭션 경계가 없다. 멘트 생성이 수십 초 걸리는 LLM 호출이라 전체를 트랜잭션으로
// 감싸면 그동안 커넥션을 붙잡게 된다. 조회 두 번과 INSERT 한 번뿐이고 서로 원자적일 이유가 없어
// 각 리포지토리 호출의 자체 트랜잭션에 맡긴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRecommendationService {

  // 최근 이 기간에 추천된 책은 후보에서 뺀다. 같은 책이 며칠 간격으로 다시 나오면 추천이 고장 난 것처럼 보인다.
  //
  // 추천 대상 책이 이 기간보다 적으면 후보가 통째로 비는데, 그때는 제외를 포기하고 전체에서 고른다.
  // 책 11권에 10일 제외면 순환하지만 어제와 다른 책이 나오고, 5권이면 제외를 포기해야 아예 멈추지 않는다.
  // 대상을 문학과 철학으로 좁힌 뒤로는 이 경로에 더 자주 걸린다.
  static final int EXCLUSION_WINDOW_DAYS = 10;

  // 멘트 생성이 실패했을 때 쓰는 폴백. 책 소개의 첫 문장이 1순위다. 그 책에 대한 실제 설명이라
  // 어떤 기본 문구보다 낫다. 소개조차 없을 때만 이 문구로 내려간다.
  static final String DEFAULT_MESSAGE = "오늘의 책으로 골라봤어요";

  // 폴백 문장이 길면 홈 카드 한 줄을 넘긴다. 상세 조회의 소개 미리보기와 같은 규칙으로 자른다.
  private static final int FALLBACK_MAX_LENGTH = 126;
  private static final String TRUNCATION_SUFFIX = "...";

  // 문장 끝만 찾는다. 마침표 뒤에 공백이나 문자열 끝이 와야 문장 경계로 본다.
  // 소수점이나 3.6처럼 숫자 사이에 낀 마침표에서 잘리지 않게 하려는 조건이다.
  private static final Pattern FIRST_SENTENCE =
      Pattern.compile("^.*?[.!?](?=\\s|$)", Pattern.DOTALL);

  private final BookRepository bookRepository;
  private final AIRecommendationRepository aiRecommendationRepository;
  private final GeminiRecommendationClient geminiRecommendationClient;

  /**
   * 해당 날짜의 추천 도서를 준비한다.
   *
   * <p>이미 있으면 아무것도 하지 않는다. 스케줄러가 두 번 돌거나 배포로 애플리케이션이 다시 떠도 하루에 두 권이 쌓이지 않아야 한다.
   */
  public void prepare(LocalDate recommendedDate) {
    if (aiRecommendationRepository.existsByRecommendedDate(recommendedDate)) {
      return;
    }

    Long bookId = pickBookId(recommendedDate);
    if (bookId == null) {
      // 문학이나 철학으로 분류된 책이 아직 한 권도 없는 상태다. 추천할 대상이 없는 것이지 실패가 아니다.
      log.warn("추천 대상 책이 없어 오늘의 추천을 건너뛴다: recommendedDate={}", recommendedDate);
      return;
    }

    Book book = bookRepository.findById(bookId).orElse(null);
    if (book == null) {
      // 후보를 고른 뒤 저장 전에 지워진 경우. 다음 실행이 다시 고른다.
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

  // 트랜잭션을 따로 열지 않는다. 저장이 INSERT 한 건이라 리포지토리 자체 트랜잭션으로 충분하고,
  // 같은 빈 안에서 부르는 메서드에 @Transactional을 붙여봐야 프록시를 타지 않아 아무 경계도 생기지 않는다.
  private void save(Book book, String message, LocalDate recommendedDate) {
    try {
      aiRecommendationRepository.save(
          AIRecommendation.builder()
              .book(book)
              .recommendationMessage(message)
              .recommendedDate(recommendedDate)
              .build());
    } catch (DataIntegrityViolationException e) {
      // 두 실행이 동시에 없다고 읽은 경합. unique 제약이 막았고 한쪽은 이미 저장했으므로 정상이다.
      log.info("같은 날짜의 추천이 이미 저장돼 건너뛴다: recommendedDate={}", recommendedDate);
    }
  }
}
