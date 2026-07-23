package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "인증/인가", description = "소셜 로그인·토큰 API")
public interface AuthControllerDocs {

  @Operation(
      summary = "소셜 로그인",
      description =
          "provider와 providerToken으로 소셜 로그인을 수행한다. "
              + "신규 회원은 생성 후 토큰을 발급하고, 탈퇴 대기(WITHDRAWN) 회원은 복구 전용 토큰만 발급한다.")
  @PostMapping("/api/v1/auth/social-login")
  ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(
      @Valid @RequestBody SocialLoginRequest request);

  @Operation(summary = "로그아웃", description = "현재 로그인된 회원의 refresh token을 무효화한다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @PostMapping("/api/v1/auth/logout")
  ResponseEntity<ApiResponse<Map<String, Object>>> logout();
}
