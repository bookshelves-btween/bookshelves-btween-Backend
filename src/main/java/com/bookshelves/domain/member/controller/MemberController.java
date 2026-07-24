package com.bookshelves.domain.member.controller;

import com.bookshelves.domain.member.dto.request.MemberUpdateRequest;
import com.bookshelves.domain.member.dto.response.MemberInfoResponse;
import com.bookshelves.domain.member.exception.MemberSuccessCode;
import com.bookshelves.domain.member.service.MemberCommandService;
import com.bookshelves.domain.member.service.MemberQueryService;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.security.AuthenticationFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberController implements MemberControllerDocs {

  private final MemberQueryService memberQueryService;
  private final MemberCommandService memberCommandService;
  private final AuthenticationFacade authenticationFacade;

  public MemberController(
      MemberQueryService memberQueryService,
      MemberCommandService memberCommandService,
      AuthenticationFacade authenticationFacade) {
    this.memberQueryService = memberQueryService;
    this.memberCommandService = memberCommandService;
    this.authenticationFacade = authenticationFacade;
  }

  @Override
  public ResponseEntity<ApiResponse<MemberInfoResponse>> getMyInfo() {
    Long memberId = authenticationFacade.getCurrentMemberId();
    MemberInfoResponse response = memberQueryService.getMyInfo(memberId);

    return ResponseEntity.ok(
        ApiResponse.onSuccess(MemberSuccessCode.MEMBER_MY_INFO_SUCCESS, response));
  }

  @Override
  public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMyInfo(MemberUpdateRequest request) {
    Long memberId = authenticationFacade.getCurrentMemberId();
    MemberInfoResponse response = memberCommandService.updateMyInfo(memberId, request);

    return ResponseEntity.ok(
        ApiResponse.onSuccess(MemberSuccessCode.MEMBER_UPDATE_SUCCESS, response));
  }
}
