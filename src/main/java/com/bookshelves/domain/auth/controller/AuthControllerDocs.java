package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.ReissueRequest;
import com.bookshelves.domain.auth.dto.request.SocialLoginRequest;
import com.bookshelves.domain.auth.dto.response.ReissueResponse;
import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

  @Operation(
      summary = "토큰 재발급",
      description =
          "refreshToken으로 access/refresh token을 재발급한다. "
              + "재발급 시 기존 refreshToken은 무효화된다(rotation).")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "재발급 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "AUTH200_3",
                "message": "토큰 재발급에 성공하였습니다.",
                "result": {
                  "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwibWVtYmVySWQiOjF9...",
                  "refreshToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwibWVtYmVySWQiOjF9...",
                  "accessTokenExpiresIn": 3600,
                  "refreshTokenExpiresIn": 1209600
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "요청 값 검증 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "COMMON400_1",
                "message": "잘못된 요청입니다.",
                "result": {
                  "refreshToken": "공백일 수 없습니다"
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "유효하지 않은 refresh token (서명/만료/타입 불일치 또는 로그아웃 등으로 이미 무효화됨)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "AUTH401_3",
                "message": "유효하지 않은 Refresh Token입니다.",
                "result": {}
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "재발급이 불가능한 회원 상태 (탈퇴 등)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "AUTH403_2",
                "message": "토큰을 재발급할 수 없는 회원 상태입니다.",
                "result": {}
              }
              """)))
  })
  @PostMapping("/api/v1/auth/reissue")
  ResponseEntity<ApiResponse<ReissueResponse>> reissue(@Valid @RequestBody ReissueRequest request);
}
