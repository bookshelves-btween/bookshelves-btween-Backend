package com.bookshelves.domain.report.controller;

import com.bookshelves.domain.report.dto.ReportCreateRequest;
import com.bookshelves.domain.report.dto.ReportCreateResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "신고", description = "신고 API")
public interface ReportControllerDocs {

  @Operation(
      summary = "채팅방 신고",
      description =
          "채팅방을 신고한다. 신고자는 accessToken으로 식별하고 요청 본문에는 chatroomId만 받는다. "
              + "에러: 404 존재하지 않는 채팅방(REPORT404_1) · 403 비참여자(REPORT403_1) · "
              + "409 중복 신고(REPORT409_1, (신고자, chatroomId) 조합 기준)")
  ResponseEntity<ApiResponse<ReportCreateResponse>> createReport(
      @Valid @RequestBody ReportCreateRequest request);
}
