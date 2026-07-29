package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.repository.AIQuestionRepository;
import com.bookshelves.domain.ai.service.QuestionVoteStore;
import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.converter.ChatConverter;
import com.bookshelves.domain.chat.dto.ChatRoomEnterResponse;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
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
            .orElseThrow(() -> new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Meeting meeting = chatRoom.getMeeting();

    // 접근 검증 기준(403·410)은 SUBSCRIBE 검증과 동일해야 한다 — validator로 단일화
    chatSubscriptionValidator.validate(chatRoom, memberId);

    // 질문 5개는 모임 시작 전에 미리 저장되므로 "가장 큰 order"가 아니라 커서로 현재 질문을 찾는다.
    // 시작 시 커서가 1로 올라가고 안전망이 질문을 보장하므로 IN_PROGRESS에서는 항상 존재, 시작 전이면 null
    AIQuestion currentQuestion =
        meeting.getStatus() == MeetingStatus.IN_PROGRESS
            ? aiQuestionRepository
                .findByMeetingIdAndQuestionOrder(meeting.getId(), meeting.getCurrentQuestionOrder())
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
        SeedQuestion.count(),
        chatMessageRepository.findAllWithSenderByChatroomId(chatroomId));
  }
}
