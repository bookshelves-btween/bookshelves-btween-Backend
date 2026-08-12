package com.bookshelves.domain.meeting.scheduler;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingTerminationService;
import com.bookshelves.global.util.ServiceTime;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 종료 시각이 지난 모임을 폴링해 서버 재시작 후에도 별도 예약 복구 없이 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingTerminationScheduler {

  private final MeetingRepository meetingRepository;
  private final MeetingTerminationService meetingTerminationService;

  @Scheduled(fixedRate = 15_000)
  public void terminateEndedMeetings() {
    LocalDateTime now = ServiceTime.now();

    List<Meeting> candidates = meetingRepository.findAllByStatus(MeetingStatus.IN_PROGRESS);
    for (Meeting meeting : candidates) {
      if (meeting.getEndDate().isAfter(now)) {
        continue;
      }
      // 한 모임의 실패가 나머지 종료 처리를 막지 않게 한다.
      try {
        meetingTerminationService.terminate(meeting.getId());
      } catch (Exception e) {
        log.error("모임 종료 처리 실패: meetingId={}", meeting.getId(), e);
      }
    }
  }
}
