package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 입장 API와 STOMP 구독에서 채팅방 접근 조건을 동일하게 검증한다.
// WebSocket 설정과의 순환 의존을 피하기 위해 리포지토리에만 의존한다.
@Component
@RequiredArgsConstructor
public class ChatSubscriptionValidator {

  private final ChatRoomRepository chatRoomRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;

  @Transactional(readOnly = true)
  public void validate(Long chatroomId, Long memberId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND));

    validate(chatRoom, memberId);
  }

  /** 이미 조회한 채팅방은 재조회하지 않고 검증한다. */
  public void validate(ChatRoom chatRoom, Long memberId) {
    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(
        chatRoom.getMeeting().getId(), memberId)) {
      throw new ChatException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }
    if (chatRoom.getMeeting().getStatus() == MeetingStatus.COMPLETED) {
      throw new ChatException(ChatErrorCode.CHATROOM_ENDED);
    }
  }
}
