package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.client.GeminiSummaryClient;
import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.repository.MeetingSummaryRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class MeetingSummaryPreparationServiceTest {

  private static final Long MEETING_ID = 1L;
  private static final Long CHATROOM_ID = 2L;
  private static final String FALLBACK_TITLE = "나눈 이야기가 적어 정리하지 못했어요";

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingSummaryRepository meetingSummaryRepository;
  @Mock private AIQuestionRepository aiQuestionRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private ChatMessageRepository chatMessageRepository;
  @Mock private GeminiSummaryClient geminiSummaryClient;
  @Mock private MeetingSummaryNotifier meetingSummaryNotifier;
  @Mock private TransactionTemplate transactionTemplate;

  private MeetingSummaryPreparationService service;

  @BeforeEach
  void setUp() {
    service =
        new MeetingSummaryPreparationService(
            meetingRepository,
            meetingSummaryRepository,
            aiQuestionRepository,
            chatRoomRepository,
            chatMessageRepository,
            geminiSummaryClient,
            meetingSummaryNotifier,
            new SyncTaskExecutor(),
            transactionTemplate);
  }

  // transactionTemplate.executeWithoutResult가 넘겨받은 콜백을 그대로 실행하게 한다
  @SuppressWarnings("unchecked")
  private void runTransactionCallbackInline() {
    willAnswer(
            invocation -> {
              invocation.getArgument(0, Consumer.class).accept(null);
              return null;
            })
        .given(transactionTemplate)
        .executeWithoutResult(any());
  }

  private Meeting meeting() {
    Meeting meeting =
        Meeting.builder()
            .book(Book.builder().isbn("9788936434595").title("아몬드").build())
            .duration(60)
            .maxParticipants(6)
            .build();
    ReflectionTestUtils.setField(meeting, "id", MEETING_ID);
    return meeting;
  }

  private ChatMessage message(String text) {
    Meeting meeting = meeting();
    ChatRoom chatRoom = ChatRoom.create(meeting);
    ReflectionTestUtils.setField(chatRoom, "id", CHATROOM_ID);
    return ChatMessage.builder()
        .chatRoom(chatRoom)
        .senderMember(Member.createSocialMember(Provider.KAKAO, "provider-1"))
        .message(text)
        .build();
  }

  // 대화가 있는 정상 경로의 준비물을 한 번에 세운다
  private void givenMeetingWithConversation() {
    runTransactionCallbackInline();
    Meeting meeting = meeting();
    ChatRoom chatRoom = ChatRoom.create(meeting);
    ReflectionTestUtils.setField(chatRoom, "id", CHATROOM_ID);

    given(meetingSummaryRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of());
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(MEETING_ID)).willReturn(Optional.of(chatRoom));
    given(chatMessageRepository.findAllWithSenderByChatroomId(CHATROOM_ID))
        .willReturn(List.of(message("윤재가 변한 게 맞나요")));
    given(aiQuestionRepository.findAllByMeetingIdOrderByQuestionOrderAsc(MEETING_ID))
        .willReturn(List.of());
  }

  private List<MeetingSummary> captureSaved() {
    ArgumentCaptor<List<MeetingSummary>> captor = ArgumentCaptor.captor();
    verify(meetingSummaryRepository).saveAll(captor.capture());
    return captor.getValue();
  }

  @Test
  void savesThreeRowsWhenModelReturnsEveryAxis() {
    givenMeetingWithConversation();
    given(geminiSummaryClient.generateSummaries(any(), any(), any()))
        .willReturn(
            Map.of(
                SummaryAxis.KEY_ARGUMENT, new GeminiSummaryClient.SummaryDraft("논점", "내용1"),
                SummaryAxis.REACTION, new GeminiSummaryClient.SummaryDraft("반응", "내용2"),
                SummaryAxis.LIFE_LINK, new GeminiSummaryClient.SummaryDraft("연결", "내용3")));

    service.prepare(MEETING_ID);

    List<MeetingSummary> saved = captureSaved();
    assertThat(saved).hasSize(3);
    assertThat(saved)
        .extracting(MeetingSummary::getAxis)
        .containsExactlyElementsOf(SummaryAxis.ordered());
    assertThat(saved).noneMatch(summary -> summary.getTitle().equals(FALLBACK_TITLE));
  }

  @Test
  void fillsMissingAxisWithFallbackTitleAndEmptyContent() {
    givenMeetingWithConversation();
    given(geminiSummaryClient.generateSummaries(any(), any(), any()))
        .willReturn(
            Map.of(SummaryAxis.KEY_ARGUMENT, new GeminiSummaryClient.SummaryDraft("논점", "내용1")));

    service.prepare(MEETING_ID);

    List<MeetingSummary> saved = captureSaved();
    // 프론트가 주제 3칸을 항상 그리므로 축이 모자라도 행은 3개여야 한다
    assertThat(saved).hasSize(3);
    assertThat(saved)
        .filteredOn(summary -> summary.getAxis() != SummaryAxis.KEY_ARGUMENT)
        .allSatisfy(
            summary -> {
              assertThat(summary.getTitle()).isEqualTo(FALLBACK_TITLE);
              assertThat(summary.getContent()).isNull();
            });
  }

  @Test
  void fillsEveryAxisWithFallbackWhenClientKeepsFailing() {
    givenMeetingWithConversation();
    willThrow(new IllegalStateException("호출 실패"))
        .given(geminiSummaryClient)
        .generateSummaries(any(), any(), any());

    service.prepare(MEETING_ID);

    List<MeetingSummary> saved = captureSaved();
    assertThat(saved).hasSize(3);
    assertThat(saved)
        .allSatisfy(summary -> assertThat(summary.getTitle()).isEqualTo(FALLBACK_TITLE));
    // 일시적 오류로 영구적인 안내 문구가 박히지 않도록 재시도한다
    verify(geminiSummaryClient, times(4)).generateSummaries(any(), any(), any());
  }

  @Test
  void skipsLlmCallWhenThereIsNoConversation() {
    runTransactionCallbackInline();
    Meeting meeting = meeting();
    given(meetingSummaryRepository.findAllByMeetingId(MEETING_ID)).willReturn(List.of());
    given(meetingRepository.findWithBookById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingRepository.findByIdForUpdate(MEETING_ID)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(MEETING_ID)).willReturn(Optional.empty());

    service.prepare(MEETING_ID);

    verify(geminiSummaryClient, never()).generateSummaries(any(), any(), any());
    assertThat(captureSaved()).hasSize(3);
  }

  @Test
  void doesNotRegenerateWhenEveryAxisIsAlreadyStored() {
    given(meetingSummaryRepository.findAllByMeetingId(MEETING_ID))
        .willReturn(
            SummaryAxis.ordered().stream()
                .map(
                    axis ->
                        MeetingSummary.builder().axis(axis).title("이미 있음").content("본문").build())
                .toList());

    service.prepare(MEETING_ID);

    verify(geminiSummaryClient, never()).generateSummaries(any(), any(), any());
    verify(meetingSummaryRepository, never()).saveAll(any());
    // 요약은 저장됐는데 알림 직전에 중단된 경우가 있어 알림은 그대로 보정한다
    verify(meetingSummaryNotifier).notifySummaryDone(MEETING_ID);
  }

  @Test
  void notifiesAfterSummariesAreStored() {
    givenMeetingWithConversation();
    given(geminiSummaryClient.generateSummaries(any(), any(), any())).willReturn(Map.of());

    service.prepare(MEETING_ID);

    verify(meetingSummaryNotifier).notifySummaryDone(MEETING_ID);
  }
}
