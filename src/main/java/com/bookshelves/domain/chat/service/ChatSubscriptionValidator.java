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

// 채팅방 접근 검증의 단일 기준 — 미존재 404 · 비참여자 403 · 종료 모임 410.
// 입장 API(ChatQueryService)와 SUBSCRIBE 인터셉터가 같은 인스턴스를 사용한다.
// StompAuthChannelInterceptor(→ WebSocketConfig 의존 사슬)에서 호출되므로 리포지토리에만
// 의존해야 한다. ChatQueryService를 물면 presence → SimpMessagingTemplate → WebSocketConfig로
// 이어지는 순환 참조가 생겨 부팅이 실패한다. (#53)
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

  /** 이미 조회된 ChatRoom에 대한 검증 — 입장 API처럼 fetch join으로 로드한 경우 재조회를 피한다. */
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
