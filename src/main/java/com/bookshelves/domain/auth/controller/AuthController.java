package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.FakeSignUpRequest;
import com.bookshelves.domain.auth.dto.request.ReissueRequest;
import com.bookshelves.domain.auth.dto.request.RestoreRequest;
import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.ReissueResponse;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthSuccessCode;
import com.bookshelves.domain.auth.service.AuthCommandService;
import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.global.apiPayload.ApiResponse;
import com.bookshelves.global.apiPayload.code.BaseSuccessCode;
import com.bookshelves.global.security.AuthenticationFacade;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthControllerDocs {

  private final AuthCommandService authCommandService;
  private final AuthenticationFacade authenticationFacade;

  public AuthController(
      AuthCommandService authCommandService, AuthenticationFacade authenticationFacade) {
    this.authCommandService = authCommandService;
    this.authenticationFacade = authenticationFacade;
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

  @Override
  public ResponseEntity<ApiResponse<SocialLoginResponse>> fakeSignUp(FakeSignUpRequest request) {
    SocialLoginResponse response = authCommandService.fakeSignUp(request);

    return ResponseEntity.ok(
        ApiResponse.onSuccess(AuthSuccessCode.AUTH_FAKE_SIGN_UP_SUCCESS, response));
  }

  @Override
  public ResponseEntity<ApiResponse<Map<String, Object>>> logout() {
    Long memberId = authenticationFacade.getCurrentMemberId();
    authCommandService.logout(memberId);

    return ResponseEntity.ok(ApiResponse.onSuccess(AuthSuccessCode.AUTH_LOGOUT_SUCCESS, Map.of()));
  }

  @Override
  public ResponseEntity<ApiResponse<ReissueResponse>> reissue(ReissueRequest request) {
    ReissueResponse response = authCommandService.reissue(request);

    return ResponseEntity.ok(ApiResponse.onSuccess(AuthSuccessCode.AUTH_REISSUE_SUCCESS, response));
  }

  @Override
  public ResponseEntity<ApiResponse<SocialLoginResponse>> restore(RestoreRequest request) {
    SocialLoginResponse response = authCommandService.restore(request);

    return ResponseEntity.ok(ApiResponse.onSuccess(AuthSuccessCode.AUTH_RESTORE_SUCCESS, response));
  }
}
