package com.bookshelves.domain.chat.service;

import com.bookshelves.domain.chat.code.ChatErrorCode;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.chat.repository.ChatRoomRepository;
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
  private final MeetingParticipantRepository meetingParticipantRepository;

  public boolean isParticipant(Long chatroomId, Long memberId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(chatroomId)
            .orElseThrow(() -> new ProjectException(ChatErrorCode.CHATROOM_NOT_FOUND));

    return meetingParticipantRepository.existsByMeetingIdAndMemberId(
        chatRoom.getMeeting().getId(), memberId);
  }
}
