package com.bookshelves.domain.report.dto;

import com.bookshelves.domain.report.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "채팅방 신고 접수 결과")
public record ReportCreateResponse(
    @Schema(description = "신고 ID", example = "15") Long id,
    @Schema(description = "신고한 채팅방 ID", example = "7") Long chatroomId,
    @Schema(description = "신고 처리 상태", example = "PENDING") ReportStatus status,
    @Schema(description = "신고 접수 시각", example = "2026-08-12T19:20:31") LocalDateTime createdAt) {}
