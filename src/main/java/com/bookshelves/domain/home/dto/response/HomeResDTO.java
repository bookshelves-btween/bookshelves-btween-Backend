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

  @Schema(description = "책 정보")
  public record BookInfo(
      Long id,
      @Schema(description = "책 상세 조회와 서재 담기의 식별자", example = "9788936434595") String isbn,
      String title,
      String author,
      String publisher,
      String coverImageUrl,
      String kdcCode,
      String kdcName) {}

  @Schema(description = "오늘의 추천 도서")
  public record RecommendedBookInfo(
      @Schema(description = "한 줄 추천 멘트", example = "감정을 배우는 소년의 조용한 성장 기록")
          String recommendationMessage,
      BookInfo book) {}

  @Schema(description = "최근 본 책")
  public record RecentBookInfo(MemberBookRecord memberBook, BookInfo book) {}

  // status는 진행률에서 파생하며 updatedAt은 최근 본 책의 선정 기준이다.
  @Schema(description = "내 서재 기록")
  public record MemberBookRecord(
      Long id,
      @Schema(description = "읽은 진행도 퍼센트", example = "70") Integer progress,
      @Schema(description = "진행률에서 파생한 상태", example = "READING") String status,
      @Schema(description = "내가 기록한 별점. 기록 전이면 null", example = "4.5") BigDecimal rating,
      @Schema(description = "기록을 마지막으로 수정한 시각") LocalDateTime updatedAt) {}

  // 요일 표시는 클라이언트가 startDate와 로케일로 구성한다.
  @Schema(description = "모집중 모임")
  public record MeetingInfo(MeetingSummary meeting, MeetingBookInfo book) {}

  @Schema(description = "모임 요약")
  public record MeetingSummary(
      Long id,
      String status,
      LocalDateTime startDate,
      Integer currentParticipants,
      Integer maxParticipants,
      @Schema(description = "진행 시간(분)", example = "30") Integer duration) {}

  // 모임 카드는 모임 상세로 이동하므로 ISBN을 포함하지 않는다.
  @Schema(description = "모임 대상 책")
  public record MeetingBookInfo(
      Long id, String title, String author, String publisher, String coverImageUrl) {}
}
