package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.event.QuestionRevealedEvent;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class QuestionRevealServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private QuestionVoteStore questionVoteStore;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private QuestionRevealService questionRevealService;

  private Meeting givenMeeting(MeetingStatus status, int currentQuestionOrder) {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(status);
    given(meeting.getCurrentQuestionOrder()).willReturn(currentQuestionOrder);
    return meeting;
  }

  @Test
  void advancesCursorAndPublishesRevealedEvent() {
    Meeting meeting = givenMeeting(MeetingStatus.IN_PROGRESS, 2);
    // revealNextQuestion() 이후의 커서를 이벤트에 담는다
    given(meeting.getCurrentQuestionOrder()).willReturn(2, 3);
    given(questionVoteStore.consumeRound(100L, 7)).willReturn(true);

    assertThat(questionRevealService.revealNext(100L, 7)).isTrue();

    verify(meeting).revealNextQuestion();
    ArgumentCaptor<QuestionRevealedEvent> captor =
        ArgumentCaptor.forClass(QuestionRevealedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new QuestionRevealedEvent(100L, 1L, 3));
  }

  @Test
  void doesNotRevealWhenAnotherPathAlreadyConsumedTheRound() {
    // 투표 경로와 이탈 재판정 경로가 같은 표를 근거로 각각 커서를 올리면 질문 하나가 통째로 건너뛰어진다
    Meeting meeting = givenMeeting(MeetingStatus.IN_PROGRESS, 2);
    given(questionVoteStore.consumeRound(100L, 7)).willReturn(false);

    assertThat(questionRevealService.revealNext(100L, 7)).isFalse();

    verify(meeting, never()).revealNextQuestion();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void doesNotRevealBeyondLastQuestion() {
    Meeting meeting = givenMeeting(MeetingStatus.IN_PROGRESS, SeedQuestion.count());

    assertThat(questionRevealService.revealNext(100L, 7)).isFalse();

    verify(meeting, never()).revealNextQuestion();
    // 상한을 넘었으면 표를 소비하지도 않는다 — 마지막 질문에서 던진 표가 사라지면 안 된다
    verify(questionVoteStore, never()).consumeRound(any(), anyInt());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void doesNotRevealWhenMeetingIsNotInProgress() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    Meeting meeting = mock(Meeting.class);
    given(chatRoomRepository.findById(100L)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(meeting.getId()).willReturn(1L);
    given(meetingRepository.findByIdForUpdate(1L)).willReturn(Optional.of(meeting));
    given(meeting.getStatus()).willReturn(MeetingStatus.COMPLETED);

    assertThat(questionRevealService.revealNext(100L, 7)).isFalse();

    verify(meeting, never()).revealNextQuestion();
  }

  @Test
  void returnsFalseWhenChatRoomIsMissing() {
    given(chatRoomRepository.findById(100L)).willReturn(Optional.empty());

    assertThat(questionRevealService.revealNext(100L, 7)).isFalse();

    verify(eventPublisher, never()).publishEvent(any());
  }
}
