package com.bookshelves.domain.report.dto;

import jakarta.validation.constraints.NotNull;

// 신고자는 accessToken으로 식별하므로 요청 본문에는 chatroomId만 받는다.
public record ReportCreateRequest(@NotNull Long chatroomId) {}
