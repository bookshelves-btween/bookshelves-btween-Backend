package com.bookshelves.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.ai.repository.MeetingSummaryRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetingQueryServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private MeetingSummaryRepository meetingSummaryRepository;
  @InjectMocks private MeetingQueryService meetingQueryService;

  @Test
  void getMeetingDetailReturnsSummaryWhenSummaryIsCompleted() {
    Long meetingId = 1L;
    Meeting meeting = meeting(meetingId, MeetingStatus.COMPLETED);
    ChatRoom chatRoom = mock(ChatRoom.class);
    MeetingSummary meetingSummary = mock(MeetingSummary.class);
    AIQuestion aiQuestion = mock(AIQuestion.class);

    given(chatRoom.getId()).willReturn(10L);
    given(meetingSummary.getAiQuestion()).willReturn(aiQuestion);
    given(meetingSummary.getContent()).willReturn("참여자들의 의견을 요약한 내용");
    given(aiQuestion.getQuestionOrder()).willReturn(1);
    given(aiQuestion.getContent()).willReturn("가장 인상 깊었던 장면은 무엇인가요?");
    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(meetingId)).willReturn(Optional.of(chatRoom));
    given(meetingSummaryRepository.findAllByMeetingIdOrderByQuestionOrder(meetingId))
        .willReturn(List.of(meetingSummary));

    MeetingDetailResDTO result = meetingQueryService.getMeetingDetail(meetingId);

    assertThat(result.chatroomId()).isEqualTo(10L);
    assertThat(result.status()).isEqualTo(MeetingStatus.COMPLETED);
    assertThat(result.meetingSummary()).hasSize(1);
    assertThat(result.meetingSummary().getFirst().questionOrder()).isEqualTo(1);
  }

  @Test
  void getMeetingDetailReturnsStatusAndNullSummaryBeforeSummaryIsCompleted() {
    Long meetingId = 1L;
    Meeting meeting = meeting(meetingId, MeetingStatus.RECRUITING);

    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(meetingId)).willReturn(Optional.empty());
    given(meetingSummaryRepository.findAllByMeetingIdOrderByQuestionOrder(meetingId))
        .willReturn(List.of());

    MeetingDetailResDTO result = meetingQueryService.getMeetingDetail(meetingId);

    assertThat(result.chatroomId()).isNull();
    assertThat(result.status()).isEqualTo(MeetingStatus.RECRUITING);
    assertThat(result.meetingSummary()).isNull();
  }

  @Test
  void getMeetingDetailReturnsNullSummaryWhenMeetingIsNotCompleted() {
    Long meetingId = 1L;
    Meeting meeting = meeting(meetingId, MeetingStatus.IN_PROGRESS);

    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(meetingId)).willReturn(Optional.empty());
    given(meetingSummaryRepository.findAllByMeetingIdOrderByQuestionOrder(meetingId))
        .willReturn(List.of(mock(MeetingSummary.class)));

    MeetingDetailResDTO result = meetingQueryService.getMeetingDetail(meetingId);

    assertThat(result.status()).isEqualTo(MeetingStatus.IN_PROGRESS);
    assertThat(result.meetingSummary()).isNull();
  }

  @Test
  void getMeetingDetailThrowsExceptionWhenMeetingDoesNotExist() {
    Long meetingId = 999L;
    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> meetingQueryService.getMeetingDetail(meetingId))
        .isInstanceOf(MeetingException.class);
    verifyNoInteractions(chatRoomRepository, meetingSummaryRepository);
  }

  private Meeting meeting(Long meetingId, MeetingStatus meetingStatus) {
    Meeting meeting = mock(Meeting.class);
    Book book = mock(Book.class);
    LocalDateTime startDate = LocalDateTime.of(2026, 7, 15, 19, 30);

    given(meeting.getId()).willReturn(meetingId);
    given(meeting.getBook()).willReturn(book);
    given(meeting.getStatus()).willReturn(meetingStatus);
    given(meeting.getStartDate()).willReturn(startDate);
    given(meeting.getDuration()).willReturn(90);
    given(meeting.getCurParticipants()).willReturn(3);
    given(meeting.getMaxParticipants()).willReturn(6);
    given(book.getId()).willReturn(101L);
    given(book.getTitle()).willReturn("혼모노");
    given(book.getDescription()).willReturn("도서 설명");
    given(book.getAuthor()).willReturn("성해나");
    given(book.getPublisher()).willReturn("창비");
    given(book.getCoverImageUrl()).willReturn("https://image.example.com/book.jpg");
    given(book.getKdcName()).willReturn("문학");
    return meeting;
  }
}
