package com.bookshelves.domain.chat.dto;

import com.bookshelves.domain.meeting.enums.MeetingStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record ChatRoomEnterResponse(
    Long chatroomId,
    Long meetingId,
    String bookTitle,
    MeetingStatus status,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    Integer maxParticipants,
    Participants participants,
    Long myMemberId,
    CurrentQuestion currentQuestion,
    Integer maxQuestions,
    Vote vote,
    List<ChatMessagePayload> messages) {

  public record Participants(int applied, int connected) {}

  public record CurrentQuestion(Long questionId, Integer questionOrder, String content) {}

  public record Vote(int currentVotes, int requiredVotes, boolean voted) {}
}
