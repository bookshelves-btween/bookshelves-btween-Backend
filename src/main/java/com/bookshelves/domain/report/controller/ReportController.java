package com.bookshelves.domain.report.controller;

import com.bookshelves.domain.report.code.ReportSuccessCode;
import com.bookshelves.domain.report.dto.ReportCreateRequest;
import com.bookshelves.domain.report.dto.ReportCreateResponse;
import com.bookshelves.domain.report.service.ReportCommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

  private final ReportCommandService reportCommandService;
  private final AuthenticationFacade authenticationFacade;

  @Override
  @PostMapping("/api/v1/reports")
  public ResponseEntity<ApiResponse<ReportCreateResponse>> createReport(
      @Valid @RequestBody ReportCreateRequest request) {
    ReportCreateResponse response =
        reportCommandService.createReport(
            request.chatroomId(), authenticationFacade.getCurrentMemberId());

    return ResponseEntity.status(ReportSuccessCode.REPORT_CREATED.getStatus())
        .body(ApiResponse.onSuccess(ReportSuccessCode.REPORT_CREATED, response));
  }
}
