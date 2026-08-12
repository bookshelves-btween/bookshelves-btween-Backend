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

// 투표와 접속자 이탈 경로에서 공통으로 사용하는 다음 질문 공개 로직.
// 별도 서비스로 분리해 ChatPresenceService와의 순환 의존을 피한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionRevealService {

  private final MeetingRepository meetingRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final QuestionVoteStore questionVoteStore;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * 다음 질문을 공개하고 성공 여부를 반환한다. 비관적 락으로 커서 증가를 직렬화하고, 같은 락 안에서 {@code expectedRound}를 소비해 같은 표로 질문이 두
   * 번 공개되는 것을 막는다.
   *
   * @param expectedRound 정족수를 판정한 시점의 라운드 번호
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
      return false;
    }

    meeting.revealNextQuestion();
    eventPublisher.publishEvent(
        new QuestionRevealedEvent(chatroomId, meetingId, meeting.getCurrentQuestionOrder()));
    return true;
  }
}
