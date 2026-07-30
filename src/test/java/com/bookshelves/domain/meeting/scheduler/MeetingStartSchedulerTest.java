package com.bookshelves.domain.meeting.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    InOrder processingOrder = inOrder(meetingRepository, meetingCommandService);
    ArgumentCaptor<LocalDateTime> deadlineCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    processingOrder
        .verify(meetingRepository)
        .findAllByStatusAndStartDateLessThanEqual(
            eq(MeetingStatus.RECRUITING), deadlineCaptor.capture());
    ArgumentCaptor<LocalDateTime> deadlineNowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    processingOrder
        .verify(meetingCommandService)
        .processRecruitmentDeadline(eq(2L), deadlineNowCaptor.capture());
    ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    processingOrder
        .verify(meetingRepository)
        .findAllByStatusInAndStartDateLessThanEqual(
            eq(List.of(MeetingStatus.RECRUITING, MeetingStatus.RECRUIT_CLOSED)),
            nowCaptor.capture());
    LocalDateTime now = nowCaptor.getValue();
    processingOrder.verify(meetingCommandService).startMeeting(1L, now);

    assertThat(deadlineCaptor.getValue()).isEqualTo(now.plusHours(6));
    assertThat(deadlineNowCaptor.getValue()).isEqualTo(now);
  }
}
