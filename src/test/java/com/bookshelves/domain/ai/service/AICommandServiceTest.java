package com.bookshelves.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.ai.code.AIErrorCode;
import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.exception.AIException;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.chat.service.ChatPresenceService;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class AICommandServiceTest {

  private static final Long MEETING_ID = 1L;
  private static final Long CHATROOM_ID = 100L;
  private static final Long MEMBER_ID = 10L;

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingParticipantRepository meetingParticipantRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private QuestionVoteStore questionVoteStore;
  @Mock private ChatPresenceService chatPresenceService;
  @Mock private QuestionRevealService questionRevealService;
  @Mock private SimpMessagingTemplate messagingTemplate;
  @InjectMocks private AICommandService aiCommandService;

  private Meeting givenInProgressMeeting(int currentQuestionOrder) {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(MEETING_ID, MEMBER_ID))
        .willReturn(true);
    given(meeting.getStatus()).willReturn(MeetingStatus.IN_PROGRESS);
    given(meeting.getCurrentQuestionOrder()).willReturn(currentQuestionOrder);
    return meeting;
  }

  private void givenChatRoom() {
    ChatRoom chatRoom = mock(ChatRoom.class);
    given(chatRoomRepository.findByMeetingId(MEETING_ID)).willReturn(Optional.of(chatRoom));
    given(chatRoom.getId()).willReturn(CHATROOM_ID);
  }

  @Test
  void revealsNextQuestionWhenQuorumIsReached() {
    givenInProgressMeeting(1);
    givenChatRoom();
    given(questionVoteStore.addVote(CHATROOM_ID, MEMBER_ID)).willReturn(true);
    given(questionVoteStore.snapshot(CHATROOM_ID))
        .willReturn(new QuestionVoteStore.VoteRound(3, 2));
    given(chatPresenceService.requiredVotes(CHATROOM_ID)).willReturn(2);
    given(questionRevealService.revealNext(CHATROOM_ID, 3)).willReturn(true);

    QuestionVoteResponse response = aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID);

    assertThat(response.currentVotes()).isEqualTo(2);
    assertThat(response.requiredVotes()).isEqualTo(2);
    assertThat(response.triggered()).isTrue();
    verify(questionRevealService).revealNext(CHATROOM_ID, 3);
  }

  @Test
  void doesNotRevealBeforeQuorum() {
    givenInProgressMeeting(1);
    givenChatRoom();
    given(questionVoteStore.addVote(CHATROOM_ID, MEMBER_ID)).willReturn(true);
    given(questionVoteStore.snapshot(CHATROOM_ID))
        .willReturn(new QuestionVoteStore.VoteRound(0, 1));
    given(chatPresenceService.requiredVotes(CHATROOM_ID)).willReturn(2);

    QuestionVoteResponse response = aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID);

    assertThat(response.triggered()).isFalse();
    verify(questionRevealService, never()).revealNext(anyLong(), anyInt());
  }

  @Test
  void doesNotRevealWhenNobodyIsConnected() {
    givenInProgressMeeting(1);
    givenChatRoom();
    given(questionVoteStore.addVote(CHATROOM_ID, MEMBER_ID)).willReturn(true);
    given(questionVoteStore.snapshot(CHATROOM_ID))
        .willReturn(new QuestionVoteStore.VoteRound(0, 1));
    // requiredVotes=0이면 정족수 판정 자체가 무의미하다
    given(chatPresenceService.requiredVotes(CHATROOM_ID)).willReturn(0);

    assertThat(aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID).triggered()).isFalse();
    verify(questionRevealService, never()).revealNext(anyLong(), anyInt());
  }

  @Test
  void rejectsDuplicateVote() {
    givenInProgressMeeting(1);
    givenChatRoom();
    given(questionVoteStore.addVote(CHATROOM_ID, MEMBER_ID)).willReturn(false);

    assertThatThrownBy(() -> aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID))
        .isInstanceOf(AIException.class)
        .hasFieldOrPropertyWithValue("errorCode", AIErrorCode.ALREADY_VOTED);
  }

  @Test
  void rejectsVoteFromNonParticipant() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(MEETING_ID, MEMBER_ID))
        .willReturn(false);

    assertThatThrownBy(() -> aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID))
        .isInstanceOf(AIException.class)
        .hasFieldOrPropertyWithValue("errorCode", AIErrorCode.VOTE_FORBIDDEN);
  }

  @Test
  void rejectsVoteWhenMeetingIsNotInProgress() {
    Meeting meeting = mock(Meeting.class);
    given(meetingRepository.findById(MEETING_ID)).willReturn(Optional.of(meeting));
    given(meetingParticipantRepository.existsByMeetingIdAndMemberId(MEETING_ID, MEMBER_ID))
        .willReturn(true);
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUIT_CLOSED);

    assertThatThrownBy(() -> aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID))
        .isInstanceOf(AIException.class)
        .hasFieldOrPropertyWithValue("errorCode", AIErrorCode.MEETING_NOT_IN_PROGRESS);
  }

  @Test
  void rejectsVoteAfterLastQuestionIsRevealed() {
    givenInProgressMeeting(SeedQuestion.count());

    assertThatThrownBy(() -> aiCommandService.voteForNewQuestion(MEETING_ID, MEMBER_ID))
        .isInstanceOf(AIException.class)
        .hasFieldOrPropertyWithValue("errorCode", AIErrorCode.QUESTION_LIMIT_REACHED);
  }
}
