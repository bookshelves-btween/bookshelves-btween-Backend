package com.bookshelves.domain.chat.dto;

import com.bookshelves.domain.meeting.enums.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "채팅방 입장 화면 구성 정보")
public record ChatRoomEnterResponse(
    @Schema(description = "채팅방 ID", example = "7") Long chatroomId,
    @Schema(description = "모임 ID", example = "21") Long meetingId,
    @Schema(description = "모임 도서 제목", example = "아몬드") String bookTitle,
    @Schema(description = "모임 상태", example = "IN_PROGRESS") MeetingStatus status,
    @Schema(description = "모임 시작 시각", example = "2026-08-12T19:00:00+09:00")
        OffsetDateTime startsAt,
    @Schema(description = "모임 종료 예정 시각", example = "2026-08-12T20:00:00+09:00")
        OffsetDateTime endsAt,
    @Schema(description = "모임 최대 참여 인원", example = "6") Integer maxParticipants,
    Participants participants,
    @Schema(description = "현재 로그인한 회원 ID", example = "12") Long myMemberId,
    @Schema(description = "현재 공개된 질문. 모임 시작 전에는 null", nullable = true)
        CurrentQuestion currentQuestion,
    @Schema(description = "모임에서 공개할 수 있는 전체 질문 수", example = "5") Integer maxQuestions,
    Vote vote,
    @Schema(description = "오래된 순서로 정렬된 전체 채팅 메시지") List<ChatMessagePayload> messages) {

  @Schema(description = "모임 참여 및 실시간 접속 인원")
  public record Participants(
      @Schema(description = "모임 신청 인원", example = "5") int applied,
      @Schema(description = "현재 채팅방 접속 인원", example = "4") int connected) {}

  @Schema(description = "현재 공개된 질문")
  public record CurrentQuestion(
      @Schema(description = "질문 ID", example = "101") Long questionId,
      @Schema(description = "질문 순서", example = "2") Integer questionOrder,
      @Schema(description = "질문 내용", example = "이 책에 별점을 준다면 몇 점이며, 그 이유는 무엇인가요?")
          String content) {}

  @Schema(description = "현재 질문 공개 투표 현황")
  public record Vote(
      @Schema(description = "현재 투표 수", example = "1") int currentVotes,
      @Schema(description = "다음 질문 공개에 필요한 투표 수", example = "2") int requiredVotes,
      @Schema(description = "현재 회원의 이번 라운드 투표 여부", example = "true") boolean voted) {}
}
