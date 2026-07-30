package com.bookshelves.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.ai.repository.MeetingSummaryRepository;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.dto.response.MeetingDetailResDTO;
import com.bookshelves.domain.meeting.dto.response.MeetingSearchResDTO;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.global.security.AuthenticationFacade;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MeetingQueryServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private ChatRoomRepository chatRoomRepository;
  @Mock private MeetingSummaryRepository meetingSummaryRepository;
  @Mock private AuthenticationFacade authenticationFacade;
  @InjectMocks private MeetingQueryService meetingQueryService;

  @Test
  void getMeetingDetailReturnsSummaryWhenSummaryIsCompleted() {
    Long meetingId = 1L;
    Meeting meeting = meeting(meetingId, MeetingStatus.COMPLETED);
    ChatRoom chatRoom = mock(ChatRoom.class);

    given(chatRoom.getId()).willReturn(10L);
    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(meetingId)).willReturn(Optional.of(chatRoom));
    // 저장 순서가 축 순서와 다르게 돌아와도 응답은 축 표시 순서로 정렬돼야 한다
    given(meetingSummaryRepository.findAllByMeetingId(meetingId))
        .willReturn(
            List.of(
                summary(SummaryAxis.LIFE_LINK, "삶과 연결"),
                summary(SummaryAxis.KEY_ARGUMENT, "핵심 논점"),
                summary(SummaryAxis.REACTION, "참여자 반응")));

    MeetingDetailResDTO result = meetingQueryService.getMeetingDetail(meetingId);

    assertThat(result.chatroomId()).isEqualTo(10L);
    assertThat(result.status()).isEqualTo(MeetingStatus.COMPLETED);
    // 프론트가 주제 3칸을 그리므로 완료된 모임은 항상 3개가 나가야 한다
    assertThat(result.meetingSummary()).hasSize(3);
    assertThat(result.meetingSummary())
        .extracting(MeetingDetailResDTO.SummaryInfo::title)
        .containsExactly("핵심 논점", "참여자 반응", "삶과 연결");
  }

  private MeetingSummary summary(SummaryAxis axis, String title) {
    return MeetingSummary.builder().axis(axis).title(title).content(title + " 본문").build();
  }

  // 완료 전에는 요약이 응답에 실리지 않으므로 조회할 이유도 없다.
  @ParameterizedTest
  @EnumSource(
      value = MeetingStatus.class,
      names = {"RECRUITING", "IN_PROGRESS"})
  void getMeetingDetailSkipsSummaryLookupUntilMeetingIsCompleted(MeetingStatus status) {
    Long meetingId = 1L;
    Meeting meeting = meeting(meetingId, status);

    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.of(meeting));
    given(chatRoomRepository.findByMeetingId(meetingId)).willReturn(Optional.empty());

    MeetingDetailResDTO result = meetingQueryService.getMeetingDetail(meetingId);

    assertThat(result.chatroomId()).isNull();
    assertThat(result.status()).isEqualTo(status);
    assertThat(result.meetingSummary()).isNull();
    verifyNoInteractions(meetingSummaryRepository);
  }

  @Test
  void getMeetingDetailThrowsExceptionWhenMeetingDoesNotExist() {
    Long meetingId = 999L;
    given(meetingRepository.findWithBookById(meetingId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> meetingQueryService.getMeetingDetail(meetingId))
        .isInstanceOf(MeetingException.class);
    verifyNoInteractions(chatRoomRepository, meetingSummaryRepository);
  }

  @Test
  void searchMeetingsReturnsMatchingMeetingsWithChatroomAndPagination() {
    Meeting meeting = searchMeeting(1L, "혼모노");
    ChatRoom chatRoom = mock(ChatRoom.class);
    given(chatRoom.getId()).willReturn(10L);
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(authenticationFacade.getCurrentMemberId()).willReturn(1001L);
    given(
            meetingRepository.findSearchableMeetings(
                eq("혼모노"),
                eq(MeetingStatus.RECRUITING),
                eq(1001L),
                argThat(
                    pageable ->
                        pageable.getSort().getOrderFor("startDate") != null
                            && pageable.getSort().getOrderFor("id") != null)))
        .willReturn(new PageImpl<>(List.of(meeting), PageRequest.of(0, 1), 2));
    given(chatRoomRepository.findAllByMeetingIdIn(List.of(1L))).willReturn(List.of(chatRoom));

    MeetingSearchResDTO result = meetingQueryService.searchMeetings("  혼모노  ", 1, 1);

    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.meetings()).hasSize(1);
    assertThat(result.meetings().getFirst().chatroomId()).isEqualTo(10L);
    assertThat(result.meetings().getFirst().book().title()).isEqualTo("혼모노");
  }

  @Test
  void getMyMeetingsReturnsLeaderMeetingsForRequestedMonth() {
    Meeting meeting = searchMeeting(1L, "아몬드");
    ChatRoom chatRoom = mock(ChatRoom.class);
    given(chatRoom.getId()).willReturn(10L);
    given(chatRoom.getMeeting()).willReturn(meeting);
    given(authenticationFacade.getCurrentMemberId()).willReturn(1001L);
    given(
            meetingRepository.findMyMeetings(
                eq(1001L), eq(true), eq(2026), eq(7), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(meeting), PageRequest.of(0, 20), 1));
    given(chatRoomRepository.findAllByMeetingIdIn(List.of(1L))).willReturn(List.of(chatRoom));

    MeetingSearchResDTO result = meetingQueryService.getMyMeetings(true, 2026, 7, 1, 20);

    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(20);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.meetings()).hasSize(1);
    assertThat(result.meetings().getFirst().chatroomId()).isEqualTo(10L);
    assertThat(result.meetings().getFirst().book().title()).isEqualTo("아몬드");
  }

  @Test
  void getMyMeetingsReturnsAllParticipatedMeetingsWithoutDateFilter() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(1001L);
    given(
            meetingRepository.findMyMeetings(
                eq(1001L), eq(false), eq(null), eq(null), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

    MeetingSearchResDTO result = meetingQueryService.getMyMeetings(false, null, null, 2, 10);

    assertThat(result.page()).isEqualTo(2);
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.meetings()).isEmpty();
    verifyNoInteractions(chatRoomRepository);
  }

  @Test
  void getMyMeetingsRejectsMonthWhenYearIsNotProvided() {
    assertThatThrownBy(() -> meetingQueryService.getMyMeetings(false, null, 7, 1, 20))
        .isInstanceOf(MeetingException.class)
        .extracting(exception -> ((MeetingException) exception).getErrorCode())
        .isEqualTo(MeetingErrorCode.MEETING_MONTH_REQUIRES_YEAR);

    verifyNoInteractions(meetingRepository, chatRoomRepository);
  }

  @Test
  void getMyMeetingsAppliesYearWithoutMonth() {
    given(authenticationFacade.getCurrentMemberId()).willReturn(1001L);
    given(
            meetingRepository.findMyMeetings(
                eq(1001L), eq(true), eq(2026), eq(null), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    MeetingSearchResDTO result = meetingQueryService.getMyMeetings(true, 2026, null, 1, 20);

    assertThat(result.meetings()).isEmpty();
    verifyNoInteractions(chatRoomRepository);
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

  private Meeting searchMeeting(Long meetingId, String title) {
    Meeting meeting = mock(Meeting.class);
    Book book = mock(Book.class);

    given(meeting.getId()).willReturn(meetingId);
    given(meeting.getBook()).willReturn(book);
    given(meeting.getStatus()).willReturn(MeetingStatus.RECRUITING);
    given(meeting.getStartDate()).willReturn(LocalDateTime.of(2026, 8, 1, 20, 0));
    given(meeting.getCurParticipants()).willReturn(3);
    given(meeting.getMaxParticipants()).willReturn(4);
    given(meeting.getDuration()).willReturn(60);
    given(book.getId()).willReturn(101L);
    given(book.getTitle()).willReturn(title);
    given(book.getCoverImageUrl()).willReturn("https://image.example.com/book.jpg");
    return meeting;
  }
}
