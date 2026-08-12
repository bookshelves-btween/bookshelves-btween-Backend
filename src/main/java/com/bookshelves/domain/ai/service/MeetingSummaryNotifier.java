package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 요약 저장과 분리된 트랜잭션에서 완료 알림을 생성한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummaryNotifier {

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final NotificationRepository notificationRepository;
  private final NotificationCommandService notificationCommandService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void notifySummaryDone(Long meetingId) {
    try {
      Meeting meeting = meetingRepository.findWithBookById(meetingId).orElse(null);
      if (meeting == null) {
        return;
      }

      List<Notification> notifications =
          meetingParticipantRepository.findAllWithMemberByMeetingId(meetingId).stream()
              .map(MeetingParticipant::getMember)
              .filter(member -> !alreadyNotified(member, meetingId))
              .map(member -> Notification.meetingSummaryDone(member, meeting))
              .toList();

      if (!notifications.isEmpty()) {
        notificationCommandService.createNotifications(notifications);
      }
    } catch (Exception e) {
      log.error("AI 요약 완료 알림 생성 실패: meetingId={}", meetingId, e);
    }
  }

  // 선조회로 중복을 줄이고, 동시 실행의 경합은 DB unique 제약으로 막는다.
  private boolean alreadyNotified(Member member, Long meetingId) {
    return notificationRepository.existsByMember_IdAndTypeAndRelatedId(
        member.getId(), NotificationType.MEETING_SUMMARY_DONE, meetingId);
  }
}
