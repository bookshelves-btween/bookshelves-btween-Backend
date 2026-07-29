package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.client.GeminiQuestionClient;
import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AIQuestionPreparationServiceTest {

  private static final Long MEETING_ID = 1L;

  @Mock private MeetingRepository meetingRepository;
  @Mock private AIQuestionRepository aiQuestionRepository;
  @Mock private GeminiQuestionClient geminiQuestionClient;
  @Mock private TransactionTemplate transactionTemplate;

  private AIQuestionPreparationService aiQuestionPreparationService;

  @BeforeEach
  void setUp() {
    aiQuestionPreparationService =
        new AIQuestionPreparationService(
            meetingRepository,
            aiQuestionRepository,
            geminiQuestionClient,
            new SyncTaskExecutor(),
            transactionTemplate);
  }

  // transactionTemplate.executeWithoutResult가 넘겨받은 콜백을 그대로 실행하게 한다
  private void runTransactionCallbackInline() {
    willAnswer(
            invocation -> {
              invocation.getArgument(0, Consumer.class).accept(null);
              return null;
            })
        .given(transactionTemplate)
        .executeWithoutResult(any());
  }

  private Meeting givenMeeting() {
    Meeting meeting = mock(Meeting.class);
    given(meeting.getId()).willReturn(MEETING_ID);
    return meeting;
  }

  private void givenBook(Meeting meeting) {
    given(meeting.getBook())
        .willReturn(
            Book.builder()
                .isbn("9788936434595")
                .title("아몬드")
                .description("감정을 느끼지 못하는 소년의 성장소설이다. 세상과 부딪히며 조금씩 변해간다.")
                .build());
  }

  private List<AIQuestion> questionsWithOrders(Integer... orders) {
    return Arrays.stream(orders)
        .map(order -> AIQuestion.builder().content("기존 질문 " + order).questionOrder(order).build())
        .toList();
  }

  private void givenExistingOrders(Integer... orders) {
    given(aiQuestionRepository.findAllByMeetingIdOrderByQuestionOrderAsc(MEETING_ID))
        .willReturn(questionsWithOrders(orders));
  }

  private List<AIQuestion> captureSavedQuestions() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AIQuestion>> captor = ArgumentCaptor.forClass(List.class);
    verify(aiQuestionRepository).saveAll(captor.capture());
    return captor.getValue();
  }

  @Test
  void savesAdaptedQuestionsAndFallsBackToSeedForTheRest() {
    Meeting meeting = givenMeeting();
    givenBook(meeting);
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    givenExistingOrders();
    given(geminiQuestionClient.adaptSeedQuestions(any(Book.class)))
        .willReturn(Map.of(2, "각색된 2번 질문입니다."));
    runTransactionCallbackInline();

    aiQuestionPreparationService.prepare(MEETING_ID);

    List<AIQuestion> saved = captureSavedQuestions();
    assertThat(saved).extracting(AIQuestion::getQuestionOrder).containsExactly(1, 2, 3, 4, 5);
    // 각색된 항목만 바뀌고 나머지는 시드 원문이 그대로 쓰인다 — 항목별 독립 폴백
    assertThat(saved.get(1).getContent()).isEqualTo("각색된 2번 질문입니다.");
    assertThat(saved.get(0).getContent()).isEqualTo(SeedQuestion.READING_IMPRESSION.getContent());
    assertThat(saved.get(4).getContent()).isEqualTo(SeedQuestion.ONE_LINE_PITCH.getContent());
  }

  @Test
  void savesSeedQuestionsWhenAdaptationFails() {
    Meeting meeting = givenMeeting();
    givenBook(meeting);
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    givenExistingOrders();
    given(geminiQuestionClient.adaptSeedQuestions(any(Book.class)))
        .willThrow(new IllegalStateException("Gemini 응답 없음"));
    runTransactionCallbackInline();

    aiQuestionPreparationService.prepare(MEETING_ID);

    // LLM 실패가 질문 준비 실패로 번지면 안 된다 — 원문 5개는 반드시 저장된다
    assertThat(captureSavedQuestions())
        .extracting(AIQuestion::getContent)
        .containsExactlyElementsOf(
            SeedQuestion.ordered().stream().map(SeedQuestion::getContent).toList());
  }

  @Test
  void skipsSaveWhenSafetyNetFilledQuestionsWhileAdapting() {
    Meeting meeting = givenMeeting();
    givenBook(meeting);
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    // 가드 통과 시점엔 비어 있었지만, 각색이 도는 사이 안전망이 5개를 채운 상황
    given(aiQuestionRepository.findAllByMeetingIdOrderByQuestionOrderAsc(MEETING_ID))
        .willReturn(List.of(), questionsWithOrders(1, 2, 3, 4, 5));
    given(geminiQuestionClient.adaptSeedQuestions(any(Book.class))).willReturn(Map.of());
    runTransactionCallbackInline();

    aiQuestionPreparationService.prepare(MEETING_ID);

    verify(aiQuestionRepository, never()).saveAll(any());
  }

  @Test
  void skipsPreparationWhenQuestionsAlreadyExist() {
    givenExistingOrders(1, 2, 3, 4, 5);

    aiQuestionPreparationService.prepare(MEETING_ID);

    verify(geminiQuestionClient, never()).adaptSeedQuestions(any());
    verify(aiQuestionRepository, never()).saveAll(any());
  }

  @Test
  void skipsPreparationWhenMeetingWasDeleted() {
    givenExistingOrders();
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.empty());

    aiQuestionPreparationService.prepare(MEETING_ID);

    verify(aiQuestionRepository, never()).saveAll(any());
  }

  @Test
  void ensureSeededFillsAllSeedQuestionsWithoutCallingLlm() {
    Meeting meeting = givenMeeting();
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    givenExistingOrders();

    aiQuestionPreparationService.ensureSeeded(MEETING_ID);

    assertThat(captureSavedQuestions()).hasSize(SeedQuestion.count());
    // 안전망은 모임 시작 트랜잭션 안에서 도므로 LLM을 부르면 안 된다
    verify(geminiQuestionClient, never()).adaptSeedQuestions(any());
  }

  @Test
  void ensureSeededFillsOnlyMissingOrders() {
    Meeting meeting = givenMeeting();
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    // 구버전 데이터로 1~3번만 남아 있는 모임 — 커서는 5까지 오르므로 4·5가 채워져야 한다
    givenExistingOrders(1, 2, 3);

    aiQuestionPreparationService.ensureSeeded(MEETING_ID);

    assertThat(captureSavedQuestions())
        .extracting(AIQuestion::getQuestionOrder)
        .containsExactly(4, 5);
  }

  @Test
  void ensureSeededDoesNothingWhenQuestionsArePrepared() {
    Meeting meeting = givenMeeting();
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    givenExistingOrders(1, 2, 3, 4, 5);

    aiQuestionPreparationService.ensureSeeded(MEETING_ID);

    verify(aiQuestionRepository, never()).saveAll(any());
  }
}
