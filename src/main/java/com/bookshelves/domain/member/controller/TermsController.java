package com.bookshelves.domain.member.controller;

import com.bookshelves.domain.member.dto.response.TermsResponse;
import com.bookshelves.domain.member.exception.TermsSuccessCode;
import com.bookshelves.domain.member.service.TermsQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TermsController implements TermsControllerDocs {

  private final TermsQueryService termsQueryService;

  public TermsController(TermsQueryService termsQueryService) {
    this.termsQueryService = termsQueryService;
  }

  @Override
  public ResponseEntity<ApiResponse<List<TermsResponse>>> getTermsList() {
    List<TermsResponse> response = termsQueryService.getTermsList();

    return ResponseEntity.ok(ApiResponse.onSuccess(TermsSuccessCode.TERMS_LIST_SUCCESS, response));
  }
}
