package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.converter.ChatConverter;
import com.bookshelves.domain.chat.dto.ChatMessageRequest;
import com.bookshelves.domain.chat.dto.ChatMessageResponse;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatMessageRepository;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
import com.bookshelves.domain.meeting.repository.MeetingParticipantRepository;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.global.exception.ProjectException;
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

  public ChatMessageResponse saveMessage(
      Long chatroomId, Long senderId, ChatMessageRequest request) {
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

    ChatMessage chatMessage =
        chatMessageRepository.save(
            ChatConverter.toChatMessage(chatRoom, sender, request.message()));

    return ChatConverter.toChatMessageResponse(chatMessage);
  }
}
