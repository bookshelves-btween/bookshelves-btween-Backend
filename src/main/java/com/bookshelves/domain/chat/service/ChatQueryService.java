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

    // 입장 API와 STOMP 구독에 같은 접근 조건을 적용한다.
    chatSubscriptionValidator.validate(chatRoom, memberId);

    // 미리 저장된 질문 중 모임 커서가 가리키는 항목을 조회한다.
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
