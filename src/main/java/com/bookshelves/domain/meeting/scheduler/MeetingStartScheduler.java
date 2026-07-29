package com.bookshelves.domain.meeting.scheduler;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    closeRecruitment(now);
    startMeetings(now);
  }

  private void closeRecruitment(LocalDateTime now) {
    LocalDateTime recruitmentDeadline = now.plusHours(Meeting.RECRUITMENT_CLOSE_HOURS_BEFORE_START);
    List<Meeting> candidates =
        meetingRepository.findAllByStatusAndStartDateLessThanEqual(
            MeetingStatus.RECRUITING, recruitmentDeadline);
    for (Meeting meeting : candidates) {
      meetingCommandService.processRecruitmentDeadline(meeting.getId(), now);
    }
  }

  private void startMeetings(LocalDateTime now) {
    List<Meeting> candidates =
        meetingRepository.findAllByStatusInAndStartDateLessThanEqual(BEFORE_START_STATUSES, now);
    for (Meeting meeting : candidates) {
      meetingCommandService.startMeeting(meeting.getId(), now);
    }
  }
}
