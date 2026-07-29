package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.event.QuestionRevealedEvent;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 다음 질문 공개(커서 +1)만 담당한다.
//
// 공개를 요청하는 경로가 둘이라 별도 빈으로 분리했다 — 투표 정족수 도달(AICommandService)과
// 접속자 이탈로 인한 정족수 재판정(ChatPresenceService). AICommandService가 requiredVotes 때문에
// ChatPresenceService에 의존하므로, 공개 로직이 거기 있으면 두 빈이 서로를 참조해 순환이 생긴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionRevealService {

  private final MeetingRepository meetingRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final QuestionVoteStore questionVoteStore;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 다음 질문을 공개한다. 실제로 공개했으면 true.
   *
   * <p>비관적 락으로 커서 증가를 직렬화하고, 락 안에서 {@code expectedRound} 라운드를 원자적으로 닫아 승자를 하나로 만든다. 락만으로는 부족하다 — 두
   * 호출 경로가 서로 다른 락 아래에서 각자 정족수를 판정하므로, 직렬화만 하면 같은 표를 근거로 커서가 두 번 올라 질문 하나가 통째로 건너뛰어진다.
   *
   * <p>라운드 소비는 커밋 전에 일어난다. 이 트랜잭션이 롤백되면 커서는 되돌아가지만 표는 복원되지 않아 재투표가 필요하다. 커밋 후에 닫으면 그 사이 들어온 요청이 같은
   * 표로 또 공개하므로, 이중 공개(질문 유실)보다 재투표를 택한 것이다.
   *
   * @param expectedRound 호출자가 정족수를 판정한 시점의 라운드 번호
   */
  @Transactional
  public boolean revealNext(Long chatroomId, int expectedRound) {
    ChatRoom chatRoom = chatRoomRepository.findById(chatroomId).orElse(null);
    if (chatRoom == null) {
      return false;
    }
    Long meetingId = chatRoom.getMeeting().getId();

    Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
    if (meeting == null
        || meeting.getStatus() != MeetingStatus.IN_PROGRESS
        || meeting.getCurrentQuestionOrder() >= SeedQuestion.count()) {
      return false;
    }
    if (!questionVoteStore.consumeRound(chatroomId, expectedRound)) {
      return false; // 다른 경로가 이미 이 라운드를 닫았다
    }

    meeting.revealNextQuestion();
    eventPublisher.publishEvent(
        new QuestionRevealedEvent(chatroomId, meetingId, meeting.getCurrentQuestionOrder()));
    return true;
  }
}
