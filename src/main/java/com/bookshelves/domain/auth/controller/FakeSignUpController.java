package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.FakeSignUpRequest;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.auth.exception.AuthSuccessCode;
import com.bookshelves.domain.auth.service.AuthCommandService;
import com.bookshelves.global.apiPayload.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// prod 프로파일에서는 이 컨트롤러 자체가 빈으로 등록되지 않는다. FAKE_SIGNUP_SECRET을
// 운영 환경변수에 잘못 채워 넣더라도, 이 경로를 처리할 핸들러가 없어 무의미해진다.
@Profile("!prod")
@RestController
public class FakeSignUpController implements FakeSignUpControllerDocs {

  private final AuthCommandService authCommandService;

  public FakeSignUpController(AuthCommandService authCommandService) {
    this.authCommandService = authCommandService;
  }

  @Override
  public ResponseEntity<ApiResponse<SocialLoginResponse>> fakeSignUp(FakeSignUpRequest request) {
    SocialLoginResponse response = authCommandService.fakeSignUp(request);

    return ResponseEntity.ok(
        ApiResponse.onSuccess(AuthSuccessCode.AUTH_FAKE_SIGN_UP_SUCCESS, response));
  }
}
