package com.bookshelves.domain.meeting.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingTerminationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MeetingTerminationSchedulerTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingTerminationService meetingTerminationService;
  @InjectMocks private MeetingTerminationScheduler meetingTerminationScheduler;

  @Test
  void limitsCandidatesAndTerminatesOnlyEndedMeetings() {
    Meeting endedMeeting = meeting(1L, LocalDateTime.now().minusMinutes(1));
    Meeting ongoingMeeting = meeting(null, LocalDateTime.now().plusMinutes(1));
    given(
            meetingRepository.findAllByStatusAndStartDateLessThanEqual(
                eq(MeetingStatus.IN_PROGRESS), any(LocalDateTime.class), any(Pageable.class)))
        .willReturn(List.of(endedMeeting, ongoingMeeting));

    meetingTerminationScheduler.terminateEndedMeetings();

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.captor();
    verify(meetingRepository)
        .findAllByStatusAndStartDateLessThanEqual(
            eq(MeetingStatus.IN_PROGRESS), any(LocalDateTime.class), pageableCaptor.capture());
    verify(meetingTerminationService).terminate(1L);
    verifyNoMoreInteractions(meetingTerminationService);

    Pageable pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageNumber()).isZero();
    assertThat(pageable.getPageSize()).isEqualTo(100);
    assertThat(pageable.getSort().getOrderFor("startDate"))
        .isNotNull()
        .satisfies(order -> assertThat(order.isAscending()).isTrue());
    assertThat(pageable.getSort().getOrderFor("id"))
        .isNotNull()
        .satisfies(order -> assertThat(order.isAscending()).isTrue());
  }

  private Meeting meeting(Long id, LocalDateTime endDate) {
    Meeting meeting = org.mockito.Mockito.mock(Meeting.class);
    if (id != null) {
      given(meeting.getId()).willReturn(id);
    }
    given(meeting.getEndDate()).willReturn(endDate);
    return meeting;
  }
}
