package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.code.AIErrorCode;
import com.bookshelves.domain.ai.converter.AIConverter;
import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.exception.AIException;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.dto.ChatVoteCountPayload;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.chat.service.ChatPresenceService;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 질문 공개 투표를 집계하고, 정족수 도달 시 미리 저장된 다음 질문을 공개한다.
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AICommandService {

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final QuestionVoteStore questionVoteStore;
  private final ChatPresenceService chatPresenceService;
  private final QuestionRevealService questionRevealService;
  private final SimpMessagingTemplate messagingTemplate;

  // 투표 반영과 현황 전송을 직렬화해 VOTE_COUNT 프레임의 역행을 막는다.
  private final Map<Long, Object> voteLocksByChatroom = new ConcurrentHashMap<>();

  public QuestionVoteResponse voteForNewQuestion(Long meetingId, Long memberId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> new MeetingException(MeetingErrorCode.MEETING_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(meetingId, memberId)) {
      throw new AIException(AIErrorCode.VOTE_FORBIDDEN);
    }
    if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      throw new AIException(AIErrorCode.MEETING_NOT_IN_PROGRESS);
    }
    if (meeting.getCurrentQuestionOrder() >= SeedQuestion.count()) {
      throw new AIException(AIErrorCode.QUESTION_LIMIT_REACHED);
    }

    ChatRoom chatRoom =
        chatRoomRepository
            .findByMeetingId(meetingId)
            .orElseThrow(() -> new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Long chatroomId = chatRoom.getId();

    int currentVotes;
    int requiredVotes;
    boolean triggered;
    synchronized (voteLocksByChatroom.computeIfAbsent(chatroomId, key -> new Object())) {
      if (!questionVoteStore.addVote(chatroomId, memberId)) {
        throw new AIException(AIErrorCode.ALREADY_VOTED);
      }

      // 표 수와 라운드를 같은 스냅샷에서 읽어 이전 판정이 새 라운드에 적용되지 않게 한다.
      QuestionVoteStore.VoteRound voteRound = questionVoteStore.snapshot(chatroomId);
      currentVotes = voteRound.votes();
      requiredVotes = chatPresenceService.requiredVotes(chatroomId);

      // 현황 전송 실패는 이미 반영된 투표를 되돌리지 않는다.
      try {
        messagingTemplate.convertAndSend(
            ChatFrame.CHATROOM_SUB_DESTINATION + chatroomId,
            ChatFrame.of(
                ChatFrame.TYPE_VOTE_COUNT,
                chatroomId,
                new ChatVoteCountPayload(currentVotes, requiredVotes)));
      } catch (Exception e) {
        log.warn("VOTE_COUNT broadcast 실패: chatroomId={}", chatroomId, e);
      }

      // 접속자가 없을 때는 질문을 공개하지 않는다.
      triggered =
          requiredVotes >= 1
              && currentVotes >= requiredVotes
              && questionRevealService.revealNext(chatroomId, voteRound.round());
    }

    return AIConverter.toQuestionVoteResponse(currentVotes, requiredVotes, triggered);
  }
}
