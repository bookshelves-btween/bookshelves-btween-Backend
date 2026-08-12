package com.bookshelves.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// 신고자는 인증 정보로 식별하므로 요청 본문에는 chatroomId만 받는다.
@Schema(description = "채팅방 신고 요청")
public record ReportCreateRequest(
    @Schema(description = "신고할 채팅방 ID", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Long chatroomId) {}
