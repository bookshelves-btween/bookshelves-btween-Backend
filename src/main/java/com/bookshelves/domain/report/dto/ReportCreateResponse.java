package com.bookshelves.domain.report.dto;

import com.bookshelves.domain.report.enums.ReportStatus;
import java.time.LocalDateTime;

public record ReportCreateResponse(
    Long id, Long chatroomId, ReportStatus status, LocalDateTime createdAt) {}
