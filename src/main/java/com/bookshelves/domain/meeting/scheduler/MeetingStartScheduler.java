package com.bookshelves.domain.meeting.scheduler;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.global.util.ServiceTime;
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
    LocalDateTime now = ServiceTime.now();
    try {
      closeRecruitment(now);
    } catch (Exception e) {
      log.error("모집 마감 대상 조회 실패", e);
    }
    try {
      startMeetings(now);
    } catch (Exception e) {
      log.error("모임 시작 대상 조회 실패", e);
    }
  }

  private void closeRecruitment(LocalDateTime now) {
    LocalDateTime recruitmentDeadline = now.plusHours(Meeting.RECRUITMENT_CLOSE_HOURS_BEFORE_START);
    List<Meeting> candidates =
        meetingRepository.findAllByStatusAndStartDateLessThanEqual(
            MeetingStatus.RECRUITING, recruitmentDeadline);
    for (Meeting meeting : candidates) {
      try {
        meetingCommandService.processRecruitmentDeadline(meeting.getId(), now);
      } catch (Exception e) {
        log.error("모집 마감 처리 실패: meetingId={}", meeting.getId(), e);
      }
    }
  }

  private void startMeetings(LocalDateTime now) {
    List<Meeting> candidates =
        meetingRepository.findAllByStatusInAndStartDateLessThanEqual(BEFORE_START_STATUSES, now);
    for (Meeting meeting : candidates) {
      try {
        meetingCommandService.startMeeting(meeting.getId(), now);
      } catch (Exception e) {
        log.error("모임 시작 처리 실패: meetingId={}", meeting.getId(), e);
      }
    }
  }
}
