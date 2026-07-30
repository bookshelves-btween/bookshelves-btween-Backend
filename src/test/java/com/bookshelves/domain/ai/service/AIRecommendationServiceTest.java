package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.client.GeminiRecommendationClient;
import com.bookshelves.domain.ai.entity.AIRecommendation;
import com.bookshelves.domain.ai.repository.AIRecommendationRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AIRecommendationServiceTest {

  private static final LocalDate TARGET_DATE = LocalDate.of(2026, 8, 1);

  @Mock private BookRepository bookRepository;
  @Mock private AIRecommendationRepository aiRecommendationRepository;
  @Mock private GeminiRecommendationClient geminiRecommendationClient;

  @InjectMocks private AIRecommendationService aiRecommendationService;

  private Book book(Long id, String description) {
    Book book = Book.builder().isbn("isbn-" + id).title("아몬드").description(description).build();
    ReflectionTestUtils.setField(book, "id", id);
    return book;
  }

  private AIRecommendation captureSaved() {
    ArgumentCaptor<AIRecommendation> captor = ArgumentCaptor.captor();
    verify(aiRecommendationRepository).save(captor.capture());
    return captor.getValue();
  }

  private void givenSingleBook(Book book) {
    given(bookRepository.findAllIds()).willReturn(List.of(book.getId()));
    given(aiRecommendationRepository.findBookIdsRecommendedSince(any())).willReturn(List.of());
    given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
  }

  @Test
  void savesGeneratedMessageForThePickedBook() {
    Book book = book(1L, "설명");
    givenSingleBook(book);
    given(geminiRecommendationClient.generateMessage(book)).willReturn("오늘은 이 책과 함께 걸어보세요");

    aiRecommendationService.prepare(TARGET_DATE);

    AIRecommendation saved = captureSaved();
    assertThat(saved.getBook()).isEqualTo(book);
    assertThat(saved.getRecommendationMessage()).isEqualTo("오늘은 이 책과 함께 걸어보세요");
    assertThat(saved.getRecommendedDate()).isEqualTo(TARGET_DATE);
  }

  @Test
  void doesNothingWhenThatDayIsAlreadyPrepared() {
    given(aiRecommendationRepository.existsByRecommendedDate(TARGET_DATE)).willReturn(true);

    aiRecommendationService.prepare(TARGET_DATE);

    // 스케줄러가 두 번 돌거나 배포로 다시 떠도 하루에 두 권이 쌓이면 안 된다
    verify(bookRepository, never()).findAllIds();
    verify(aiRecommendationRepository, never()).save(any());
  }

  @Test
  void skipsWhenNoBookHasBeenSeeded() {
    given(bookRepository.findAllIds()).willReturn(List.of());

    aiRecommendationService.prepare(TARGET_DATE);

    verify(aiRecommendationRepository, never()).save(any());
  }

  @Test
  void excludesBooksRecommendedWithinTheWindow() {
    given(bookRepository.findAllIds()).willReturn(List.of(1L, 2L, 3L));
    given(
            aiRecommendationRepository.findBookIdsRecommendedSince(
                TARGET_DATE.minusDays(AIRecommendationService.EXCLUSION_WINDOW_DAYS)))
        .willReturn(List.of(1L, 2L));
    Book remaining = book(3L, "설명");
    given(bookRepository.findById(3L)).willReturn(Optional.of(remaining));
    given(geminiRecommendationClient.generateMessage(remaining)).willReturn("남은 한 권이 뽑힌다");

    aiRecommendationService.prepare(TARGET_DATE);

    assertThat(captureSaved().getBook()).isEqualTo(remaining);
  }

  @Test
  void ignoresExclusionWhenEveryBookWasRecommendedRecently() {
    // 책이 제외 기간보다 적으면 후보가 통째로 빈다. 그때까지 추천을 멈추면 홈이 계속 비어 있게 된다.
    given(bookRepository.findAllIds()).willReturn(List.of(1L, 2L));
    given(aiRecommendationRepository.findBookIdsRecommendedSince(any()))
        .willReturn(List.of(1L, 2L));
    Book book = book(1L, "설명");
    given(bookRepository.findById(any())).willReturn(Optional.of(book));
    given(geminiRecommendationClient.generateMessage(book)).willReturn("그래도 한 권은 나온다");

    aiRecommendationService.prepare(TARGET_DATE);

    assertThat(captureSaved().getRecommendationMessage()).isEqualTo("그래도 한 권은 나온다");
  }

  @Test
  void picksDifferentBooksAcrossRunsWhenCandidatesRemain() {
    // 후보가 여럿인데 늘 같은 책이 나오면 제외 창이 하는 일이 없어진다.
    // 5권 중 30회가 전부 같은 책일 확률은 5 * (1/5)^30이라 이 단언은 흔들리지 않는다.
    given(bookRepository.findAllIds()).willReturn(List.of(1L, 2L, 3L, 4L, 5L));
    given(aiRecommendationRepository.findBookIdsRecommendedSince(any())).willReturn(List.of());
    given(bookRepository.findById(any()))
        .willAnswer(invocation -> Optional.of(book(invocation.getArgument(0), "설명")));
    given(geminiRecommendationClient.generateMessage(any())).willReturn("어느 책이 뽑혀도 멘트는 붙는다");

    for (int i = 0; i < 30; i++) {
      aiRecommendationService.prepare(TARGET_DATE);
    }

    ArgumentCaptor<AIRecommendation> captor = ArgumentCaptor.captor();
    verify(aiRecommendationRepository, times(30)).save(captor.capture());
    assertThat(captor.getAllValues().stream().map(saved -> saved.getBook().getId()).distinct())
        .hasSizeGreaterThan(1);
  }

  @Test
  void fallsBackToFirstSentenceOfDescriptionWhenGenerationFails() {
    Book book = book(1L, "감정을 느끼지 못하는 소년의 이야기다. 두 번째 문장은 쓰지 않는다.");
    givenSingleBook(book);
    willThrow(new IllegalStateException("Gemini 응답 JSON 파싱에 실패했습니다."))
        .given(geminiRecommendationClient)
        .generateMessage(book);

    aiRecommendationService.prepare(TARGET_DATE);

    // 기본 문구보다 그 책에 대한 실제 설명이 낫다
    assertThat(captureSaved().getRecommendationMessage()).isEqualTo("감정을 느끼지 못하는 소년의 이야기다.");
  }

  @Test
  void fallsBackToFirstSentenceWhenGeneratedMessageFailsValidation() {
    Book book = book(1L, "첫 문장이다. 두 번째 문장.");
    givenSingleBook(book);
    given(geminiRecommendationClient.generateMessage(book)).willReturn(null);

    aiRecommendationService.prepare(TARGET_DATE);

    assertThat(captureSaved().getRecommendationMessage()).isEqualTo("첫 문장이다.");
  }

  @Test
  void fallsBackToDefaultMessageWhenDescriptionIsBlank() {
    Book book = book(1L, "  ");
    givenSingleBook(book);
    given(geminiRecommendationClient.generateMessage(book)).willReturn(null);

    aiRecommendationService.prepare(TARGET_DATE);

    assertThat(captureSaved().getRecommendationMessage())
        .isEqualTo(AIRecommendationService.DEFAULT_MESSAGE);
  }

  @Test
  void truncatesOverlongFallbackSentence() {
    Book book = book(1L, "가".repeat(200) + ".");
    givenSingleBook(book);
    given(geminiRecommendationClient.generateMessage(book)).willReturn(null);

    aiRecommendationService.prepare(TARGET_DATE);

    String message = captureSaved().getRecommendationMessage();
    // 컬럼 상한을 넘겨 INSERT가 실패하는 일이 없어야 한다
    assertThat(message).hasSize(129).endsWith("...");
    assertThat(message.length()).isLessThanOrEqualTo(AIRecommendation.MAX_MESSAGE_LENGTH);
  }

  @Test
  void swallowsUniqueViolationFromConcurrentRun() {
    Book book = book(1L, "설명");
    givenSingleBook(book);
    given(geminiRecommendationClient.generateMessage(book)).willReturn("동시에 두 실행이 저장을 시도한다");
    willThrow(new DataIntegrityViolationException("uk_ai_recommendation_date"))
        .given(aiRecommendationRepository)
        .save(any());

    // 경합에서 진 쪽이 예외를 올리면 스케줄러 로그가 실패로 오염된다. 이미 저장됐으므로 정상이다.
    aiRecommendationService.prepare(TARGET_DATE);
  }
}
