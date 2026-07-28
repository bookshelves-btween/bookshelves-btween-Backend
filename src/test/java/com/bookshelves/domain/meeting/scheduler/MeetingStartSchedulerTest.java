package com.bookshelves.domain.meeting.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeetingStartSchedulerTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingCommandService meetingCommandService;
  @InjectMocks private MeetingStartScheduler meetingStartScheduler;

  @Test
  void startsClosedMeetingAndDeletesUnderstaffedMeeting() {
    Meeting closedMeeting = mock(Meeting.class);
    Meeting understaffedMeeting = mock(Meeting.class);
    given(closedMeeting.getId()).willReturn(1L);
    given(closedMeeting.canStart()).willReturn(true);
    given(understaffedMeeting.getId()).willReturn(2L);
    given(understaffedMeeting.canStart()).willReturn(false);
    given(
            meetingRepository.findAllByStatusInAndStartDateLessThanEqual(
                any(), any(LocalDateTime.class)))
        .willReturn(List.of(closedMeeting, understaffedMeeting));

    meetingStartScheduler.startScheduledMeetings();

    verify(meetingCommandService).startMeeting(eq(1L), any());
    verify(meetingCommandService).deleteUnderstaffedMeeting(eq(2L), any());
  }
}
