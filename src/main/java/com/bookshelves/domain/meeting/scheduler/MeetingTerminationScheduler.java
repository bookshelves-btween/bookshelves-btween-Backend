package com.bookshelves.domain.meeting.scheduler;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.service.MeetingTerminationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 종료 시각이 지난 진행 중 모임을 주기적으로 종료 처리한다(폴링).
// 개별 예약 대신 폴링을 쓰는 이유: 서버 재시작에도 복구가 필요 없고 상태 관리가 단순하다.
// 최대 1분 지연은 허용 범위(프론트는 로컬 타이머로 이미 표시상 종료, 실제 종료는 이 배치가 확정).
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingTerminationScheduler {

  private final MeetingRepository meetingRepository;
  private final MeetingTerminationService meetingTerminationService;

  @Scheduled(fixedRate = 60_000)
  public void terminateEndedMeetings() {
    LocalDateTime now = LocalDateTime.now();

    List<Meeting> candidates = meetingRepository.findAllByStatus(MeetingStatus.IN_PROGRESS);
    for (Meeting meeting : candidates) {
      if (meeting.getEndDate().isAfter(now)) {
        continue; // 아직 종료 시각 전
      }
      // 모임 단위로 격리 — 한 건 실패가 나머지 종료 처리를 막지 않도록 한다
      try {
        meetingTerminationService.terminate(meeting.getId());
      } catch (Exception e) {
        log.error("모임 종료 처리 실패: meetingId={}", meeting.getId(), e);
      }
    }
  }
}
