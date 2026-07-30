package com.bookshelves.domain.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "홈 화면 조회 결과")
public record HomeResDTO(
    MemberInfo member,
    @Schema(description = "추천 도서의 노출 날짜", example = "2026-07-31") LocalDate recommendedAt,
    @Schema(description = "오늘의 추천 도서. 추천할 책이 아직 없으면 null") RecommendedBookInfo recommendedBook,
    @Schema(description = "최근 본 책. 서재에 담은 책이 없으면 null") RecentBookInfo recentBook,
    @Schema(description = "지금 참여할 수 있는 모집중 모임. 없으면 빈 배열") List<MeetingInfo> meetings) {

  @Schema(description = "회원 정보")
  public record MemberInfo(String nickname) {}

  @Schema(description = "오늘의 추천 도서")
  public record RecommendedBookInfo(
      @Schema(description = "한 줄 추천 멘트", example = "감정을 배우는 소년의 조용한 성장 기록")
          String recommendationMessage,
      Long bookId,
      String title,
      String author,
      String publisher,
      String kdcName,
      String coverImageUrl) {}

  // status는 내려보내지 않는다. member_book에 그런 컬럼이 없고 진행률에서 파생되는 값이며,
  // 이 화면은 진행률 자체를 그린다.
  @Schema(description = "최근 본 책")
  public record RecentBookInfo(
      Long bookId,
      String title,
      String author,
      String publisher,
      String coverImageUrl,
      @Schema(description = "내가 기록한 별점. 기록 전이면 null", example = "4.5") BigDecimal rating,
      @Schema(description = "읽은 진행도 퍼센트", example = "70") Integer progress) {}

  // 요일은 startDate에서 클라이언트가 만든다. 서버가 문자열로 내려보내면 로케일 처리가 둘로 갈린다.
  @Schema(description = "모집중 모임")
  public record MeetingInfo(
      Long meetingId,
      String title,
      String coverImageUrl,
      LocalDateTime startDate,
      Integer currentParticipants,
      Integer maxParticipants) {}
}
