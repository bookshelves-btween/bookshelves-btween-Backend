package com.bookshelves.domain.meeting.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.event.MeetingCreatedEvent;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.global.util.ServiceTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class MeetingStartTaskRegistrarTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingCommandService meetingCommandService;
  @Mock private TaskScheduler taskScheduler;
  @Mock private ScheduledFuture<?> scheduledFuture;

  private MeetingStartTaskRegistrar registrar;

  @BeforeEach
  void setUp() {
    registrar =
        new MeetingStartTaskRegistrar(meetingRepository, meetingCommandService, taskScheduler);
    lenient()
        .doReturn(scheduledFuture)
        .when(taskScheduler)
        .schedule(any(Runnable.class), any(Instant.class));
  }

  @Test
  void schedulesCreatedMeetingAtServiceZoneInstant() {
    LocalDateTime startDate = LocalDateTime.of(2026, 8, 12, 20, 0);

    registrar.scheduleCreatedMeeting(new MeetingCreatedEvent(1L, startDate));

    ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
    assertThat(instantCaptor.getValue()).isEqualTo(startDate.atZone(ServiceTime.ZONE).toInstant());
  }

  @Test
  void restoresFutureMeetingsAfterApplicationStarts() {
    Meeting meeting = mock(Meeting.class);
    LocalDateTime startDate = ServiceTime.now().plusHours(1);
    given(meeting.getId()).willReturn(2L);
    given(meeting.getStartDate()).willReturn(startDate);
    given(
            meetingRepository.findAllByStatusInAndStartDateAfter(
                any(List.class), any(LocalDateTime.class)))
        .willReturn(List.of(meeting));

    registrar.restoreFutureMeetings();

    verify(taskScheduler)
        .schedule(
            any(Runnable.class),
            org.mockito.ArgumentMatchers.eq(startDate.atZone(ServiceTime.ZONE).toInstant()));
  }

  @Test
  void scheduledTaskStartsMeeting() {
    LocalDateTime startDate = ServiceTime.now().plusMinutes(1);
    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

    registrar.schedule(3L, startDate);
    verify(taskScheduler).schedule(taskCaptor.capture(), any(Instant.class));
    taskCaptor.getValue().run();

    verify(meetingCommandService).startMeeting(eq(3L), any(LocalDateTime.class));
  }

  @Test
  void removesTaskThatCompletesBeforeItIsRegistered() {
    TaskScheduler immediateScheduler = mock(TaskScheduler.class);
    ScheduledFuture<?> completedFuture = mock(ScheduledFuture.class);
    MeetingStartTaskRegistrar immediateRegistrar =
        new MeetingStartTaskRegistrar(meetingRepository, meetingCommandService, immediateScheduler);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              invocation.<Runnable>getArgument(0).run();
              return completedFuture;
            })
        .when(immediateScheduler)
        .schedule(any(Runnable.class), any(Instant.class));

    immediateRegistrar.schedule(4L, ServiceTime.now().minusSeconds(1));
    immediateRegistrar.schedule(4L, ServiceTime.now().minusSeconds(1));

    verify(completedFuture, never()).cancel(false);
  }

  @Test
  void oldTaskCannotRemoveNewRegistrationForSameMeeting() {
    ScheduledFuture<?> firstFuture = mock(ScheduledFuture.class);
    ScheduledFuture<?> secondFuture = mock(ScheduledFuture.class);
    ScheduledFuture<?> thirdFuture = mock(ScheduledFuture.class);
    doReturn(firstFuture, secondFuture, thirdFuture)
        .when(taskScheduler)
        .schedule(any(Runnable.class), any(Instant.class));
    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

    registrar.schedule(5L, ServiceTime.now().plusMinutes(1));
    registrar.schedule(5L, ServiceTime.now().plusMinutes(2));
    verify(taskScheduler, org.mockito.Mockito.times(2))
        .schedule(taskCaptor.capture(), any(Instant.class));

    taskCaptor.getAllValues().getFirst().run();
    registrar.schedule(5L, ServiceTime.now().plusMinutes(3));

    verify(secondFuture).cancel(false);
  }
}
