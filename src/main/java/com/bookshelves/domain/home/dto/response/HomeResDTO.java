package com.bookshelves.domain.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "홈 화면 조회 결과")
public record HomeResDTO(
    @Schema(description = "로그인한 회원 정보") MemberInfo member,
    @Schema(description = "반환된 추천 도서의 노출 기준일. 저장된 추천이 없으면 null", example = "2026-07-31")
        LocalDate recommendedAt,
    @Schema(description = "오늘의 추천 도서. 저장된 추천이 없으면 null", nullable = true)
        RecommendedBookInfo recommendedBook,
    @Schema(description = "가장 최근에 수정한 서재 기록. 서재가 비어 있으면 null", nullable = true)
        RecentBookInfo recentBook,
    @Schema(description = "현재 참여할 수 있는 모집 중 모임. 최대 3건이며 없으면 빈 배열") List<MeetingInfo> meetings) {

  @Schema(description = "회원 정보")
  public record MemberInfo(@Schema(description = "회원 닉네임", example = "책 먹는 여우") String nickname) {}

  // 엔티티별로 객체를 나눈다. 각 객체의 PK 필드명은 id로 통일한다.
  @Schema(description = "책 정보")
  public record BookInfo(
      @Schema(description = "책 ID", example = "1") Long id,
      @Schema(description = "책 상세 조회와 서재 담기의 식별자", example = "9788936434595") String isbn,
      @Schema(description = "책 제목", example = "아몬드") String title,
      @Schema(description = "저자", example = "손원평") String author,
      @Schema(description = "출판사", example = "창비") String publisher,
      @Schema(description = "표지 이미지 URL", example = "https://example.com/almond.jpg")
          String coverImageUrl,
      @Schema(description = "세부 KDC 코드", example = "813") String kdcCode,
      @Schema(description = "세부 KDC 분류명", example = "한국소설") String kdcName) {}

  @Schema(description = "오늘의 추천 도서")
  public record RecommendedBookInfo(
      @Schema(description = "한 줄 추천 멘트", example = "감정을 배우는 소년의 조용한 성장 기록")
          String recommendationMessage,
      @Schema(description = "추천 도서 정보") BookInfo book) {}

  @Schema(description = "최근 본 책")
  public record RecentBookInfo(
      @Schema(description = "회원의 서재 기록") MemberBookRecord memberBook,
      @Schema(description = "책 정보") BookInfo book) {}

  // status는 member_book에 컬럼이 없고 진행률에서 파생한다. 서재 목록과 같은 규칙을 쓰기 위해
  // MemberBookStatus.from을 거친다.
  // updatedAt은 최근 본 책을 고른 기준값이다. 어떤 기록이 언제 갱신돼 이 책이 뽑혔는지 드러낸다.
  @Schema(description = "내 서재 기록")
  public record MemberBookRecord(
      @Schema(description = "서재 기록 ID", example = "10") Long id,
      @Schema(description = "독서 진행률(%)", example = "70") Integer progress,
      @Schema(description = "진행률에서 파생한 상태", example = "READING") String status,
      @Schema(description = "내가 기록한 별점. 기록 전이면 null", example = "4.5") BigDecimal rating,
      @Schema(description = "서재 기록을 마지막으로 수정한 시각", example = "2026-07-30T04:30:00")
          LocalDateTime updatedAt) {}

  // 요일은 startDate에서 클라이언트가 만든다. 서버가 문자열로 내려보내면 로케일 처리가 둘로 갈린다.
  @Schema(description = "모집중 모임")
  public record MeetingInfo(
      @Schema(description = "모임 정보") MeetingSummary meeting,
      @Schema(description = "모임 대상 책 정보") MeetingBookInfo book) {}

  @Schema(description = "모임 요약")
  public record MeetingSummary(
      @Schema(description = "모임 ID", example = "21") Long id,
      @Schema(description = "모임 상태", example = "RECRUITING") String status,
      @Schema(description = "모임 시작 시각", example = "2026-08-02T19:00:00") LocalDateTime startDate,
      @Schema(description = "현재 신청 인원", example = "4") Integer currentParticipants,
      @Schema(description = "최대 참여 인원", example = "6") Integer maxParticipants,
      @Schema(description = "진행 시간(분)", example = "30") Integer duration) {}

  // 모임 카드는 모임 상세로만 이동하므로 책 식별자가 필요 없다. isbn을 담지 않는다.
  @Schema(description = "모임 대상 책")
  public record MeetingBookInfo(
      @Schema(description = "책 ID", example = "2") Long id,
      @Schema(description = "책 제목", example = "혼모노") String title,
      @Schema(description = "저자", example = "성해나") String author,
      @Schema(description = "출판사", example = "창비") String publisher,
      @Schema(description = "표지 이미지 URL", example = "https://example.com/honmono.jpg")
          String coverImageUrl) {}
}
