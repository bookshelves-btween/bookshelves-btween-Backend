package com.bookshelves.domain.meeting.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetingStartSchedulerTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingCommandService meetingCommandService;
  @InjectMocks private MeetingStartScheduler meetingStartScheduler;

  @Test
  void closesRecruitmentBeforeStartingScheduledMeetings() {
    Meeting closedMeeting = mock(Meeting.class);
    Meeting recruitmentDeadlineMeeting = mock(Meeting.class);
    given(closedMeeting.getId()).willReturn(1L);
    given(recruitmentDeadlineMeeting.getId()).willReturn(2L);
    given(
            meetingRepository.findAllByStatusAndStartDateLessThanEqual(
                eq(MeetingStatus.RECRUITING), any(LocalDateTime.class)))
        .willReturn(List.of(recruitmentDeadlineMeeting));
    given(
            meetingRepository.findAllByStatusInAndStartDateLessThanEqual(
                eq(List.of(MeetingStatus.RECRUITING, MeetingStatus.RECRUIT_CLOSED)),
                any(LocalDateTime.class)))
        .willReturn(List.of(closedMeeting));

    meetingStartScheduler.startScheduledMeetings();

    ArgumentCaptor<LocalDateTime> deadlineCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(meetingRepository)
        .findAllByStatusAndStartDateLessThanEqual(
            eq(MeetingStatus.RECRUITING), deadlineCaptor.capture());
    ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(meetingRepository)
        .findAllByStatusInAndStartDateLessThanEqual(
            eq(List.of(MeetingStatus.RECRUITING, MeetingStatus.RECRUIT_CLOSED)),
            nowCaptor.capture());
    LocalDateTime now = nowCaptor.getValue();

    assertThat(deadlineCaptor.getValue()).isEqualTo(now.plusHours(6));
    verify(meetingCommandService).processRecruitmentDeadline(2L, now);
    verify(meetingCommandService).startMeeting(1L, now);
  }
}
