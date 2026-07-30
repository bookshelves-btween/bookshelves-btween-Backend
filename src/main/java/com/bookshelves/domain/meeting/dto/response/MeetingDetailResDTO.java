package com.bookshelves.domain.meeting.dto.response;

import com.bookshelves.domain.meeting.enums.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 상세 조회 결과")
public record MeetingDetailResDTO(
    Long id,
    Long chatroomId,
    MeetingStatus status,
    LocalDateTime startDate,
    Integer duration,
    Integer currentParticipants,
    Integer maxParticipants,
    BookInfo book,
    List<SummaryInfo> meetingSummary) {

  @Schema(description = "모임 도서 정보")
  public record BookInfo(
      Long id,
      String title,
      String description,
      String author,
      String publisher,
      String coverImageUrl,
      String kdcName) {}

  // 순번을 배열 순서로만 두지 않는다. 화면이 주제 1, 2, 3 칸을 그리는데 배열 인덱스에 기대게 하면
  // 서버에서 정렬이 바뀌었을 때 응답만 봐서는 무엇이 어긋났는지 알 수 없다.
  @Schema(description = "모임 요약 주제")
  public record SummaryInfo(
      @Schema(description = "주제 순번. 1부터 3까지 항상 순서대로 나갑니다", example = "1") Integer order,
      String title,
      String summary) {}
}
