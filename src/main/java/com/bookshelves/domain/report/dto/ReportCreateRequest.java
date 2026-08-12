package com.bookshelves.domain.report.dto;

import jakarta.validation.constraints.NotNull;

// 신고자는 인증 정보로 식별한다.
public record ReportCreateRequest(@NotNull Long chatroomId) {}
