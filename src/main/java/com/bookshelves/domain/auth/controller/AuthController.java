package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthSuccessCode;
import com.bookshelves.domain.auth.service.AuthCommandService;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthControllerDocs {

  private final AuthCommandService authCommandService;

  public AuthController(AuthCommandService authCommandService) {
    this.authCommandService = authCommandService;
  }

  @Override
  public ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(SocialLoginRequest request) {
    SocialLoginResponse response = authCommandService.socialLogin(request);
    BaseSuccessCode successCode =
        response.getMemberStatus() == MemberStatus.WITHDRAWN
            ? AuthSuccessCode.AUTH_WITHDRAWN_ACCOUNT
            : AuthSuccessCode.AUTH_SOCIAL_LOGIN_SUCCESS;

    return ResponseEntity.status(successCode.getStatus())
        .body(ApiResponse.onSuccess(successCode, response));
  }
}
