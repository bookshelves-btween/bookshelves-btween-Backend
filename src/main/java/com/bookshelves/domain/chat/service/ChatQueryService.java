package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
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

  // 모임당 AI 질문 최대 개수 — AI 새 질문 생성 투표의 상한과 공유
  private static final int MAX_QUESTIONS = 5;

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;
  private final AIQuestionRepository aiQuestionRepository;
  private final ChatPresenceService chatPresenceService;

  public ChatRoomEnterResponse enterChatRoom(Long chatroomId, Long memberId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findByIdWithMeetingAndBook(chatroomId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Meeting meeting = chatRoom.getMeeting();

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(meeting.getId(), memberId)) {
      throw new ProjectException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }
    if (meeting.getStatus() == MeetingStatus.COMPLETED) {
      throw new ProjectException(ChatErrorCode.CHATROOM_ENDED);
    }

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
        chatPresenceService.requiredVotes(chatroomId),
        currentQuestion,
        MAX_QUESTIONS,
        chatMessageRepository.findAllWithSenderByChatroomId(chatroomId));
  }

  // SUBSCRIBE 권한 검증 — 입장 API와 동일 기준 (미존재 404 · 비참여자 403 · 종료 모임 410)
  public void validateSubscription(Long chatroomId, Long memberId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(
        chatRoom.getMeeting().getId(), memberId)) {
      throw new ProjectException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }
    if (chatRoom.getMeeting().getStatus() == MeetingStatus.COMPLETED) {
      throw new ProjectException(ChatErrorCode.CHATROOM_ENDED);
    }
  }
}
