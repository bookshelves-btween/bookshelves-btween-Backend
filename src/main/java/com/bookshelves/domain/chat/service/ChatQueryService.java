package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.service.AIQuestionGenerationService;
import com.bookshelves.domain.ai.service.QuestionVoteStore;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.converter.ChatConverter;
import com.bookshelves.domain.chat.dto.ChatRoomEnterResponse;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.global.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatQueryService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final AIQuestionRepository aiQuestionRepository;
  private final ChatPresenceService chatPresenceService;
  private final ChatSubscriptionValidator chatSubscriptionValidator;
  private final QuestionVoteStore questionVoteStore;

  public ChatRoomEnterResponse enterChatRoom(Long chatroomId, Long memberId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findByIdWithMeetingAndBook(chatroomId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Meeting meeting = chatRoom.getMeeting();

    // 접근 검증 기준(403·410)은 SUBSCRIBE 검증과 동일해야 한다 — validator로 단일화
    chatSubscriptionValidator.validate(chatRoom, memberId);

    // 모임 시작 시 질문 1개 자동 생성이 보장되어 IN_PROGRESS에서는 항상 존재, 시작 전이면 null
    AIQuestion currentQuestion =
        meeting.getStatus() == MeetingStatus.IN_PROGRESS
            ? aiQuestionRepository
                .findTopByMeetingIdOrderByQuestionOrderDesc(meeting.getId())
                .orElse(null)
            : null;

    return ChatConverter.toChatRoomEnterResponse(
        chatRoom,
        memberId,
        meetingParticipantRepository.countByMeetingId(meeting.getId()),
        chatPresenceService.countConnected(chatroomId),
        questionVoteStore.countVotes(chatroomId),
        chatPresenceService.requiredVotes(chatroomId),
        questionVoteStore.hasVoted(chatroomId, memberId),
        currentQuestion,
        AIQuestionGenerationService.MAX_QUESTIONS,
        chatMessageRepository.findAllWithSenderByChatroomId(chatroomId));
  }
}
