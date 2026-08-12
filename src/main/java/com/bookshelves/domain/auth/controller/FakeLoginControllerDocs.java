package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.FakeLoginRequest;
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
public interface FakeLoginControllerDocs {

  @Operation(
      summary = "테스트 로그인 토큰 발급 (임시)",
      description =
          """
          소셜 로그인 없이 이미 존재하는 회원의 `accessToken`과 `refreshToken`을 발급합니다.
          iOS 앱이 아직 배포되지 않아 소셜 로그인 플로우를 태울 수 없는 채점 환경에서, Swagger로
          특정 회원의 인증 상태를 재현하기 위한 임시 API입니다.

          요청의 `secret`은 서버 환경 변수 `FAKE_SIGNUP_SECRET`과 일치해야 합니다. 환경 변수가
          설정되지 않았거나 값이 일치하지 않으면 요청을 거부합니다.

          `memberId`는 이미 존재하는 회원이어야 하며, 존재하지 않으면 404를 반환합니다. 회원 상태는
          검사하지 않고 발급하며, 실제 요청 시점의 접근 제어는 서버가 별도로 수행합니다.

          발급된 `refreshToken`은 서버에 등록되어 `POST /api/v1/auth/reissue`로 재발급할 수 있습니다.
          """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "테스트 로그인 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": true,
                "code": "AUTH200_5",
                "message": "테스트 로그인에 성공했습니다.",
                "result": {
                  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
                  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
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
        responseCode = "400",
        description = "memberId 누락 또는 형식 오류",
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
                  "memberId": "must be greater than 0"
                }
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "403",
        description = "비밀값 불일치 또는 서버에 비밀값이 설정되지 않음",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "AUTH403_3",
                "message": "테스트 로그인 비밀값이 올바르지 않습니다.",
                "result": {}
              }
              """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "존재하지 않는 회원",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
              {
                "isSuccess": false,
                "code": "MEMBER404_1",
                "message": "회원을 찾을 수 없습니다.",
                "result": {}
              }
              """)))
  })
  @PostMapping("/api/v1/auth/fake-login")
  ResponseEntity<ApiResponse<SocialLoginResponse>> fakeLogin(
      @Valid @RequestBody FakeLoginRequest request);
}
