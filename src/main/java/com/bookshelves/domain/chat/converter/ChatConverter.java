package com.bookshelves.domain.chat.converter;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.chat.dto.ChatMessagePayload;
import com.bookshelves.domain.chat.dto.ChatRoomEnterResponse;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.global.util.ServiceTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConverter {

  public static ChatMessage toChatMessage(ChatRoom chatRoom, Member sender, String content) {
    return ChatMessage.builder().chatRoom(chatRoom).senderMember(sender).message(content).build();
  }

  public static ChatMessagePayload toChatMessagePayload(ChatMessage chatMessage) {
    return new ChatMessagePayload(
        chatMessage.getId(),
        chatMessage.getSenderMember().getId(),
        chatMessage.getSenderMember().getNickname(),
        chatMessage.getSenderMember().getNicknameAnimal(),
        chatMessage.getSenderMember().getProfileBackgroundColor(),
        chatMessage.getMessage(),
        toOffset(chatMessage.getCreatedAt()));
  }

  public static ChatRoomEnterResponse toChatRoomEnterResponse(
      ChatRoom chatRoom,
      Long myMemberId,
      int applied,
      int connected,
      int currentVotes,
      int requiredVotes,
      boolean voted,
      AIQuestion currentQuestion,
      int maxQuestions,
      List<ChatMessage> messages) {
    Meeting meeting = chatRoom.getMeeting();
    OffsetDateTime startsAt = toOffset(meeting.getStartDate());

    return new ChatRoomEnterResponse(
        chatRoom.getId(),
        meeting.getId(),
        meeting.getBook().getTitle(),
        meeting.getStatus(),
        startsAt,
        startsAt.plusMinutes(meeting.getDuration()),
        meeting.getMaxParticipants(),
        new ChatRoomEnterResponse.Participants(applied, connected),
        myMemberId,
        toCurrentQuestion(currentQuestion),
        maxQuestions,
        new ChatRoomEnterResponse.Vote(currentVotes, requiredVotes, voted),
        messages.stream().map(ChatConverter::toChatMessagePayload).toList());
  }

  private static ChatRoomEnterResponse.CurrentQuestion toCurrentQuestion(AIQuestion question) {
    if (question == null) {
      return null;
    }
    return new ChatRoomEnterResponse.CurrentQuestion(
        question.getId(), question.getQuestionOrder(), question.getContent());
  }

  private static OffsetDateTime toOffset(java.time.LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.atZone(ServiceTime.ZONE).toOffsetDateTime();
  }
}
