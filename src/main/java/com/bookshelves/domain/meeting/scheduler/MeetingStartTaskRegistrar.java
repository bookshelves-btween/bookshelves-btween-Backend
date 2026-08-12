package com.bookshelves.domain.meeting.scheduler;

import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.event.MeetingCreatedEvent;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.global.util.ServiceTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 모임 시작 시각에 맞춰 상태 전환을 실행한다. 주기 배치는 예약 누락 복구용으로 별도 유지
@Slf4j
@Component
public class MeetingStartTaskRegistrar {

  private static final List<MeetingStatus> BEFORE_START_STATUSES =
      List.of(MeetingStatus.RECRUITING, MeetingStatus.RECRUIT_CLOSED);

  private final MeetingRepository meetingRepository;
  private final MeetingCommandService meetingCommandService;
  private final TaskScheduler meetingStartTaskScheduler;
  private final Map<Long, ScheduledTaskRegistration> scheduledTasks = new ConcurrentHashMap<>();

  public MeetingStartTaskRegistrar(
      MeetingRepository meetingRepository,
      MeetingCommandService meetingCommandService,
      @Qualifier("meetingStartTaskScheduler") TaskScheduler meetingStartTaskScheduler) {
    this.meetingRepository = meetingRepository;
    this.meetingCommandService = meetingCommandService;
    this.meetingStartTaskScheduler = meetingStartTaskScheduler;
  }

  // 인메모리 예약은 재시작 시 사라지므로 DB의 미래 모임을 재등록
  @EventListener(ApplicationReadyEvent.class)
  public void restoreFutureMeetings() {
    LocalDateTime now = ServiceTime.now();
    try {
      meetingRepository
          .findAllByStatusInAndStartDateAfter(BEFORE_START_STATUSES, now)
          .forEach(meeting -> schedule(meeting.getId(), meeting.getStartDate()));
    } catch (Exception e) {
      // 기동 복구가 실패해도 애플리케이션은 시작하고 기존 폴링이 누락된 시작 처리
      log.error("미래 모임 시작 예약 복구 실패", e);
    }
  }

  // 롤백된 모임이 예약되는 것을 막기 위해 생성 트랜잭션 커밋 후에만 등록
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void scheduleCreatedMeeting(MeetingCreatedEvent event) {
    schedule(event.meetingId(), event.startDate());
  }

  void schedule(Long meetingId, LocalDateTime startDate) {
    Instant startInstant = startDate.atZone(ServiceTime.ZONE).toInstant();
    ScheduledTaskRegistration registration = new ScheduledTaskRegistration();
    ScheduledFuture<?> newTask;
    try {
      newTask =
          meetingStartTaskScheduler.schedule(
              () -> startMeeting(meetingId, registration), startInstant);
    } catch (RuntimeException e) {
      log.error("모임 시작 예약 등록 실패: meetingId={}, startDate={}", meetingId, startDate, e);
      return;
    }
    if (newTask == null) {
      log.error("모임 시작 예약 등록 실패: meetingId={}, startDate={}", meetingId, startDate);
      return;
    }

    registration.setFuture(newTask);
    ScheduledTaskRegistration previousRegistration = scheduledTasks.put(meetingId, registration);
    if (previousRegistration != null) {
      previousRegistration.cancel();
    }

    // 과거 시각 예약은 schedule() 호출 안에서 즉시 완료될 수 있으므로 등록 후 다시 정리한다.
    if (registration.isCompleted()) {
      scheduledTasks.remove(meetingId, registration);
    }
  }

  private void startMeeting(Long meetingId, ScheduledTaskRegistration registration) {
    try {
      meetingCommandService.startMeeting(meetingId, ServiceTime.now());
    } catch (Exception e) {
      // 실패하거나 서버가 실행 직후 종료돼도 기존 폴링 스케줄러가 다시 처리
      log.error("예약된 모임 시작 처리 실패: meetingId={}", meetingId, e);
    } finally {
      registration.complete();
      scheduledTasks.remove(meetingId, registration);
    }
  }

  private static final class ScheduledTaskRegistration {

    private final AtomicBoolean completed = new AtomicBoolean();
    private volatile ScheduledFuture<?> future;

    void setFuture(ScheduledFuture<?> future) {
      this.future = future;
    }

    void complete() {
      completed.set(true);
    }

    boolean isCompleted() {
      return completed.get();
    }

    void cancel() {
      ScheduledFuture<?> scheduledFuture = future;
      if (scheduledFuture != null) {
        scheduledFuture.cancel(false);
      }
    }
  }
}
