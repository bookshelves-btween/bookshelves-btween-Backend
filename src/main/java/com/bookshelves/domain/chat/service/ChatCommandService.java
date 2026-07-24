package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.converter.ChatConverter;
import com.bookshelves.domain.chat.dto.ChatMessagePayload;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.enums.MeetingStatus;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
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

  // 저장 성공 시 broadcast용 payload 반환. 모임이 진행 중(IN_PROGRESS)이 아니면
  // 명세에 따라 저장·broadcast 없이 무시한다 (빈 Optional).
  public Optional<ChatMessagePayload> saveMessage(Long chatroomId, Long senderId, String content) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND));
    Member sender =
        memberRepository
            .findById(senderId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.SENDER_NOT_FOUND));

    if (!meetingParticipantRepository.existsByMeetingIdAndMemberId(
        chatRoom.getMeeting().getId(), sender.getId())) {
      throw new ProjectException(ChatErrorCode.CHATROOM_FORBIDDEN);
    }

    if (chatRoom.getMeeting().getStatus() != MeetingStatus.IN_PROGRESS) {
      return Optional.empty();
    }

    ChatMessage chatMessage =
        chatMessageRepository.save(ChatConverter.toChatMessage(chatRoom, sender, content));

    return Optional.of(ChatConverter.toChatMessagePayload(chatMessage));
  }
}
