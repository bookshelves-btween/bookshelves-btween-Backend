package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.converter.ChatConverter;
import com.bookshelves.domain.chat.dto.ChatMessagePayload;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.exception.ChatException;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatCommandService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final MemberRepository memberRepository;
  private final MeetingParticipantRepository meetingParticipantRepository;

  // 진행 중인 모임에만 메시지를 저장하고 전송용 payload를 반환한다.
  public Optional<ChatMessagePayload> saveMessage(Long chatroomId, Long senderId, String content) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ChatException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Member sender =
        memberRepository
            .findById(senderId)
            .orElseThrow(() -> new ChatException(ChatErrorCode.SENDER_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(
        chatRoom.getMeeting().getId(), sender.getId())) {
      throw new ChatException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }

    if (chatRoom.getMeeting().getStatus() != MeetingStatus.IN_PROGRESS) {
      return Optional.empty();
    }

    ChatMessage chatMessage =
        chatMessageRepository.save(ChatConverter.toChatMessage(chatRoom, sender, content));

    return Optional.of(ChatConverter.toChatMessagePayload(chatMessage));
  }
}
