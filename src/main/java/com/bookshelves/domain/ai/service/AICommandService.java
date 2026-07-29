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

// 질문 공개 투표. 클라이언트 계약은 그대로 "새 질문 생성 요청"이지만,
// 서버는 이미 저장된 다음 질문의 커서를 올릴 뿐이라 모임 진행 중 LLM 호출이 없다.
// 덕분에 생성권 선점·실패 라운드 복구·동시 생성 충돌 같은 경합이 구조적으로 사라졌다.
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

  // 채팅방별 투표 직렬화 락 — 표 반영·카운트·VOTE_COUNT 전송을 하나의 구간으로 묶어
  // 동시 투표 시 "2표 프레임 뒤에 1표 프레임"처럼 카운트가 역행하는 전송을 막는다
  private final Map<Long, Object> voteLocksByChatroom = new ConcurrentHashMap<>();

  // 투표(내 액션)는 HTTP로 받고, 현황·새 질문은 SUB 프레임으로 전파한다 — 명세 구조.
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
    // 마지막 질문까지 공개된 뒤에는 더 올릴 커서가 없다
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

      // 표 수와 라운드는 같은 스냅샷에서 읽어야 한다 — 따로 읽으면 이전 라운드의 표 수로 새 라운드를 소비한다
      QuestionVoteStore.VoteRound voteRound = questionVoteStore.snapshot(chatroomId);
      currentVotes = voteRound.votes();
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

      // 접속자가 0명이면 requiredVotes=0 — 이때는 정족수 판정 자체가 무의미하므로 공개하지 않는다
      triggered =
          requiredVotes >= 1
              && currentVotes >= requiredVotes
              && questionRevealService.revealNext(chatroomId, voteRound.round());
    }

    return AIConverter.toQuestionVoteResponse(currentVotes, requiredVotes, triggered);
  }
}
