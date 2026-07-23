package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "로그인 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "AUTH200_1",
                "message": "소셜 로그인에 성공했습니다.",
                "result": {
                  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwibWVtYmVySWQiOjF9...",
                  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwibWVtYmVySWQiOjF9...",
                  "accessTokenExpiresIn": 3600,
                  "refreshTokenExpiresIn": 1209600,
                  "restoreToken": null,
                  "restoreTokenExpiresIn": null,
                  "memberStatus": "ACTIVE",
                  "scheduledDeletionAt": null
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "202",
        description = "탈퇴 대기(WITHDRAWN) 계정 — 복구 전용 토큰만 발급",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "AUTH202_1",
                "message": "탈퇴 대기 중인 계정입니다.",
                "result": {
                  "accessToken": null,
                  "refreshToken": null,
                  "accessTokenExpiresIn": null,
                  "refreshTokenExpiresIn": null,
                  "restoreToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwibWVtYmVySWQiOjF9...",
                  "restoreTokenExpiresIn": 600,
                  "memberStatus": "WITHDRAWN",
                  "scheduledDeletionAt": "2026-08-13T14:30:00+09:00"
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 값 검증 실패 또는 지원하지 않는 provider",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "요청 값 검증 실패",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {
                  "providerToken": "공백일 수 없습니다"
                }
              }
              """),
                  @ExampleObject(
                      name = "지원하지 않는 provider",
                      value =
                          """
              {
                "isSuccess": false,
                "code": "AUTH400_1",
                "message": "지원하지 않는 소셜 로그인 제공자입니다.",
                "result": {}
              }
              """)
                })),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "유효하지 않은 소셜 토큰",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "AUTH401_1",
                "message": "유효하지 않은 소셜 토큰입니다.",
                "result": {}
              }
              """)))
  })
  @PostMapping("/api/v1/auth/social-login")
  ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(
      @Valid @RequestBody SocialLoginRequest request);

  @Operation(summary = "로그아웃", description = "현재 로그인된 회원의 refresh token을 무효화한다.")
  @SecurityRequirement(name = "JWT TOKEN")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "로그아웃 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "AUTH200_2",
                "message": "로그아웃에 성공하였습니다.",
                "result": {}
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "인증 실패 — access token이 없거나, 만료·서명 불일치 등으로 유효하지 않음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "AUTH401_2",
                "message": "유효하지 않은 Access Token입니다.",
                "result": null
              }
              """)))
  })
  @PostMapping("/api/v1/auth/logout")
  ResponseEntity<ApiResponse<Map<String, Object>>> logout();
}
