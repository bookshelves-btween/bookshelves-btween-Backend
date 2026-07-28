package com.bookshelves.domain.meeting.scheduler;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingStartScheduler {

  // 시작 전 상태인 모임만 스케줄링 대상으로 조회한다.
  private static final List<MeetingStatus> BEFORE_START_STATUSES =
    List.of(MeetingStatus.RECRUITING, MeetingStatus.RECRUIT_CLOSED);

  private final MeetingRepository meetingRepository;
  private final MeetingCommandService meetingCommandService;

  @Scheduled(fixedRate = 60_000)
  public void startScheduledMeetings() {
    LocalDateTime now = LocalDateTime.now();
    List<Meeting> candidates =
      meetingRepository.findAllByStatusInAndStartDateLessThanEqual(BEFORE_START_STATUSES, now);
    for (Meeting meeting : candidates) {
      try {
        // 최소 인원(3명)을 충족한 모임은 시작하고, 미달 모임은 알림 저장 후 삭제한다.
        if (meeting.canStart()) {
          boolean started = meetingCommandService.startMeeting(meeting.getId(), now);
        } else {
          boolean deleted = meetingCommandService.deleteUnderstaffedMeeting(meeting.getId(), now);
        }
      } catch (Exception e) {
        log.error("모임 시작 처리 실패: meetingId={}", meeting.getId(), e);
      }
    }
  }
}
