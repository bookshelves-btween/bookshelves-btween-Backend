package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.notification.entity.Notification;
import com.bookshelves.domain.notification.enums.NotificationType;
import com.bookshelves.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 요약 완료 알림 생성.
//
// 별도 빈으로 둔 이유가 둘이다. 요약 저장 트랜잭션과 분리해 알림 실패가 요약을 되돌리지 않게 하고,
// 프록시를 거쳐야 REQUIRES_NEW가 실제로 새 트랜잭션을 연다. 같은 빈 안에서 자기 메서드를 호출하면
// 프록시를 타지 않아 의도한 경계가 생기지 않는다.
//
// 여기서 만드는 것은 DB 알림 레코드이고 기기 푸시가 아니다. 저장소에 FCM 전송 클라이언트가 없다.
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSummaryNotifier {

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final NotificationRepository notificationRepository;

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
        notificationRepository.saveAll(notifications);
      }
    } catch (Exception e) {
      // 요약은 이미 저장됐고 사용자는 모임 상세에서 확인할 수 있다. 알림 실패로 요약을 되돌리지 않는다.
      log.error("AI 요약 완료 알림 생성 실패: meetingId={}", meetingId, e);
    }
  }

  // 중복 이벤트로 준비가 두 번 돌면 같은 회원에게 같은 알림이 두 건 쌓인다.
  //
  // 이 조회는 흔한 중복(앞선 실행이 이미 알림을 만든 경우)을 값싸게 걸러낸다. 두 실행이 동시에
  // 없다고 읽는 경합은 이것만으로 막지 못하므로 (member_id, type, related_id) unique 제약이
  // 최종 방어를 맡는다. 경합에서 진 쪽은 커밋 시 실패하고 호출부가 로그를 남긴다.
  private boolean alreadyNotified(Member member, Long meetingId) {
    return notificationRepository.existsByMember_IdAndTypeAndRelatedId(
        member.getId(), NotificationType.MEETING_SUMMARY_DONE, meetingId);
  }
}
