package com.bookshelves.domain.meeting.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MeetingCreateReqDTO(
    @Schema(description = "모임 시작 날짜", example = "2026-08-01")
        @NotNull(message = "시작 날짜는 필수입니다.")
        @FutureOrPresent(message = "시작 날짜는 오늘 이후여야 합니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
    @Schema(description = "모임 시작 시간(HH:mm)", example = "20:00", type = "string", format = "time")
        @NotNull(message = "시작 시간은 필수입니다.")
        @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "시작 시간은 HH:mm 형식이어야 합니다.")
        String startTime,
    @Schema(description = "최대 참여 인원", example = "4")
        @NotNull(message = "최대 참여 인원은 필수입니다.")
        @Positive(message = "최대 참여 인원은 1명 이상이어야 합니다.")
        Integer maxParticipants,
    @Schema(description = "모임 진행 시간(분)", example = "60")
        @NotNull(message = "진행 시간은 필수입니다.")
        @Positive(message = "진행 시간은 1분 이상이어야 합니다.")
        Integer duration) {}
