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

// 상태 전환과 노쇼 확정을 커밋한 뒤 모임 종료 프레임을 전송한다.
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

  // 종료 프레임 전송과 요약 준비에 사용하는 내부 이벤트.
  public record MeetingEndedEvent(Long chatroomId, Long meetingId) {}

  // 비관적 락으로 종료 처리를 직렬화해 노쇼와 종료 이벤트의 중복 생성을 막는다.
  @Transactional
  public void terminate(Long meetingId) {
    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    // 이미 종료되거나 삭제된 모임은 건너뛴다.
    if (meeting == null || meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      return;
    }

    meeting.complete();

    List<MeetingParticipant> notAttended =
        meetingParticipantRepository.findNotAttendedByMeetingId(meeting.getId());
    List<NoShowEvent> noShows =
        notAttended.stream()
            .map(mp -> NoShowEvent.builder().meeting(meeting).member(mp.getMember()).build())
            .toList();
    noShowEventRepository.saveAll(noShows);

    // 종료 프레임은 트랜잭션 커밋 후 전송한다.
    chatRoomRepository
        .findByMeetingId(meeting.getId())
        .ifPresent(
            chatRoom ->
                eventPublisher.publishEvent(
                    new MeetingEndedEvent(chatRoom.getId(), meeting.getId())));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void broadcastMeetingEnded(MeetingEndedEvent event) {
    // 전송 실패 시 클라이언트는 재입장 과정에서 커밋된 종료 상태를 확인한다.
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
