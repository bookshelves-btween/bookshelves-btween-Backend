package com.bookshelves.domain.ai.service;

import com.bookshelves.domain.ai.code.AIErrorCode;
import com.bookshelves.domain.ai.converter.AIConverter;
import com.bookshelves.domain.ai.dto.QuestionVoteResponse;
import com.bookshelves.domain.ai.exception.AIException;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.dto.ChatFrame;
import com.bookshelves.domain.chat.dto.ChatVoteCountPayload;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.chat.service.ChatPresenceService;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.exception.MeetingException;
import com.bookshelves.domain.meeting.exception.code.MeetingErrorCode;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.meeting.repository.MeetingRepository;
import com.bookshelves.global.exception.ProjectException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AICommandService {

  private final MeetingRepository meetingRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final ChatRoomRepository chatRoomRepository;
  private final AIQuestionRepository aiQuestionRepository;
  private final QuestionVoteStore questionVoteStore;
  private final ChatPresenceService chatPresenceService;
  private final AIQuestionGenerationService aiQuestionGenerationService;
  private final SimpMessagingTemplate messagingTemplate;

  // 채팅방별 투표 직렬화 락 — 표 반영·카운트·VOTE_COUNT 전송을 하나의 구간으로 묶어
  // 동시 투표 시 "2표 프레임 뒤에 1표 프레임"처럼 카운트가 역행하는 전송을 막는다
  private final Map<Long, Object> voteLocksByChatroom = new ConcurrentHashMap<>();

  // 투표(내 액션)는 HTTP로 받고, 현황·새 질문은 SUB 프레임으로 전파한다 — 명세 구조.
  // 투표 반영 시 VOTE_COUNT를 전원 broadcast하고, 정족수 도달이면 비동기 질문 생성을 시작한다.
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
    if (aiQuestionRepository.countByMeetingId(meetingId)
        >= AIQuestionGenerationService.MAX_QUESTIONS) {
      throw new AIException(AIErrorCode.QUESTION_LIMIT_REACHED);
    }

    ChatRoom chatRoom =
        chatRoomRepository
            .findByMeetingId(meetingId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Long chatroomId = chatRoom.getId();

    int currentVotes;
    int requiredVotes;
    boolean triggered;
    synchronized (voteLocksByChatroom.computeIfAbsent(chatroomId, k -> new Object())) {
      switch (questionVoteStore.addVote(chatroomId, memberId)) {
        case DUPLICATE -> throw new AIException(AIErrorCode.ALREADY_VOTED);
        case GENERATING -> throw new AIException(AIErrorCode.QUESTION_GENERATING);
        case ADDED -> {}
      }

      currentVotes = questionVoteStore.countVotes(chatroomId);
      requiredVotes = chatPresenceService.requiredVotes(chatroomId);

      // 실시간 현황 전파는 best-effort — 전송 실패가 이미 반영된 투표를 실패로 둔갑시키면 안 된다
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

      // 정족수 판정과 생성권 선점(라운드 닫기)까지 같은 락 구간에서 끝낸다 —
      // 락 밖으로 빼면 선점 전에 끼어든 투표가 닫혀야 할 라운드에 표를 더한다.
      // 접속자가 0명이면 requiredVotes=0 — 이때는 정족수 판정 자체가 무의미하므로 트리거하지 않는다
      triggered = requiredVotes >= 1 && currentVotes >= requiredVotes;
      if (triggered) {
        aiQuestionGenerationService.requestGeneration(chatroomId, requiredVotes);
      }
    }

    return AIConverter.toQuestionVoteResponse(currentVotes, requiredVotes, triggered);
  }
}
