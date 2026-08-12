package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.FakeLoginRequest;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthSuccessCode;
import com.bookshelves.domain.auth.service.AuthCommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// iOS 앱 미배포 상태에서 Swagger 채점을 위해 소셜 로그인 없이 기존 회원의 토큰을 발급한다.
// FAKE_SIGNUP_SECRET이 비어 있으면 항상 거부되므로, 서버 환경 변수를 비우면 봉인된다.
@RestController
public class FakeLoginController implements FakeLoginControllerDocs {

  private final AuthCommandService authCommandService;

  public FakeLoginController(AuthCommandService authCommandService) {
    this.authCommandService = authCommandService;
  }

  @Override
  public ResponseEntity<ApiResponse<SocialLoginResponse>> fakeLogin(FakeLoginRequest request) {
    SocialLoginResponse response = authCommandService.fakeLogin(request);

    return ResponseEntity.ok(
        ApiResponse.onSuccess(AuthSuccessCode.AUTH_FAKE_LOGIN_SUCCESS, response));
  }
}
