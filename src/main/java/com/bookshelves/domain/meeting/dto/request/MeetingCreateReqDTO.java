package com.bookshelves.domain.meeting.dto.request;

import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.global.util.ServiceTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Schema(description = "모임 생성 요청")
public record MeetingCreateReqDTO(
    @Schema(description = "ISBN10 또는 ISBN13", example = "9788966262281")
        @NotBlank(message = "ISBN은 필수입니다.")
        String isbn,
    @Schema(description = "모임 시작 날짜", example = "2026-08-01")
        @NotNull(message = "시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
    @Schema(description = "모임 시작 시간(HH:mm)", example = "20:00", type = "string", format = "time")
        @NotNull(message = "시작 시간은 필수입니다.")
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "시작 시간은 HH:mm 형식이어야 합니다.")
        String startTime,
    @Schema(description = "최대 참여 인원(3~6명)", example = "4", minimum = "3", maximum = "6")
        @NotNull(message = "최대 참여 인원은 필수입니다.")
        @Min(value = Meeting.MIN_PARTICIPANTS, message = "최대 참여 인원은 3명 이상이어야 합니다.")
        @Max(value = Meeting.MAX_PARTICIPANTS, message = "최대 참여 인원은 6명 이하여야 합니다.")
        Integer maxParticipants,
    @Schema(
            description = "모임 진행 시간(분). 5분 단위로 입력",
            example = "60",
            minimum = "5",
            maximum = "60",
            multipleOf = 5)
        @NotNull(message = "진행 시간은 필수입니다.")
        @Min(value = Meeting.MIN_DURATION_MINUTES, message = "진행 시간은 5분 이상이어야 합니다.")
        @Max(value = Meeting.MAX_DURATION_MINUTES, message = "진행 시간은 60분 이하여야 합니다.")
        Integer duration) {

  @AssertTrue(message = "모임은 현재 시각으로부터 7시간 이후에 시작해야 합니다.")
  @JsonIgnore
  @Schema(hidden = true)
  public boolean isValidMeetingStartDateTime() {
    if (startDate == null || startTime == null) {
      return true;
    }

    try {
      LocalDateTime meetingStart = LocalDateTime.of(startDate, LocalTime.parse(startTime));
      LocalDateTime earliestStart =
          ServiceTime.now()
              .truncatedTo(ChronoUnit.MINUTES)
              .plusHours(Meeting.MIN_HOURS_BEFORE_START);
      return !meetingStart.isBefore(earliestStart);
    } catch (DateTimeParseException exception) {
      return true;
    }
  }

  @AssertTrue(message = "진행 시간은 5분 단위여야 합니다.")
  @JsonIgnore
  @Schema(hidden = true)
  public boolean isValidDurationUnit() {
    return duration == null || duration % Meeting.DURATION_UNIT_MINUTES == 0;
  }
}
