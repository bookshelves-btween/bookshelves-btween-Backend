package com.bookshelves.domain.meeting.service;

import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.dto.ChatSystemPayload;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.entity.MeetingParticipant;
import com.bookshelves.domain.meeting.entity.NoShowEvent;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.domain.meeting.repository.NoShowEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 모임 하나의 종료 처리 — 상태 전환·노쇼 확정을 한 트랜잭션으로 커밋하고,
// 커밋이 끝난 뒤에만 SYSTEM 프레임을 broadcast한다(커밋 전 전파 시 상태 불일치 방지).
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingTerminationService {

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final NoShowEventRepository noShowEventRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final ApplicationEventPublisher eventPublisher;

  // 커밋 후 broadcast를 위해 사용하는 내부 이벤트
  // meetingId는 요약 준비가 쓴다. 종료된 모임의 대화와 질문을 찾으려면 모임 식별자가 필요하다.
  public record MeetingEndedEvent(Long chatroomId, Long meetingId) {}

  // 스케줄러가 넘긴 엔티티는 detached이므로 트랜잭션 안에서 ID로 다시 로드해 관리 상태로 만든다.
  // 비관적 락(findByIdForUpdate)으로 종료 처리를 직렬화한다 — 다중 인스턴스/재실행에서 두 트랜잭션이
  // 모두 IN_PROGRESS를 읽고 노쇼·SYSTEM 프레임을 중복 생성하는 것을 막는다. 뒤 트랜잭션은 락 해제 후
  // 재조회에서 COMPLETED를 보고 건너뛴다.
  @Transactional
  public void terminate(Long meetingId) {
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    // 재조회 사이 상태가 바뀌었으면(이미 종료·삭제) 건너뛴다 — 폴링 재실행에 대한 멱등 가드
    if (meeting == null || meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      return;
    }

    meeting.complete();

    // 노쇼 확정 — 출석하지 않은 참여자에 대해 NoShowEvent 생성
    List<MeetingParticipant> notAttended =
        meetingParticipantRepository.findNotAttendedByMeetingId(meeting.getId());
    List<NoShowEvent> noShows =
        notAttended.stream()
            .map(mp -> NoShowEvent.builder().meeting(meeting).member(mp.getMember()).build())
            .toList();
    noShowEventRepository.saveAll(noShows);

    // 종료 알림은 커밋 후에 내보낸다 — 롤백 시 존재하지 않는 종료를 전파하지 않도록
    chatRoomRepository
        .findByMeetingId(meeting.getId())
        .ifPresent(
            chatRoom ->
                eventPublisher.publishEvent(
                    new MeetingEndedEvent(chatRoom.getId(), meeting.getId())));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void broadcastMeetingEnded(MeetingEndedEvent event) {
    // 종료는 이미 커밋됨 — 프레임 전송 실패 시 클라이언트는 재입장(입장 API)으로 종료 상태를 확인한다
    try {
      messagingTemplate.convertAndSend(
          ChatFrame.CHATROOM_SUB_DESTINATION + event.chatroomId(),
          ChatFrame.of(
              ChatFrame.TYPE_SYSTEM, event.chatroomId(), ChatSystemPayload.meetingEnded()));
    } catch (Exception e) {
      log.error("SYSTEM(MEETING_ENDED) broadcast 실패: chatroomId={}", event.chatroomId(), e);
    }
  }
}
