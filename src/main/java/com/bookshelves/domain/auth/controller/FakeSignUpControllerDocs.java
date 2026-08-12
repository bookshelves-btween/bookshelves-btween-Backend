package com.bookshelves.domain.auth.controller;

import com.bookshelves.domain.auth.dto.request.FakeSignUpRequest;
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
public interface FakeSignUpControllerDocs {

  @Operation(
      summary = "테스트 회원 토큰 발급 (임시)",
      description =
          """
          소셜 로그인 없이 테스트 회원을 생성하고 토큰을 발급합니다. 여러 시뮬레이터에서 서로 다른
          회원으로 모임 참여와 채팅을 검증하기 위한 임시 API입니다.

          앱에서는 이 API를 호출하지 않습니다. Swagger에서 직접 호출해 받은 `accessToken`과
          `refreshToken`을 테스트 기기에 설정해 사용합니다.

          요청의 `secret`은 서버 환경 변수 `FAKE_SIGNUP_SECRET`과 일치해야 합니다. 환경 변수가 설정되지
          않았거나 값이 일치하지 않으면 요청을 거부합니다.

          같은 `key`로 다시 호출하면 기존 테스트 회원의 토큰을 발급하고, 처음 사용하는 `key`이면 새 회원을
          생성합니다. 테스트 회원은 닉네임이 설정된 `ACTIVE` 상태로 생성되므로 온보딩이 필요하지 않습니다.
          `key`는 `tester-1`부터 `tester-99`까지의 형식만 허용합니다.

          `prod` 프로파일에서는 엔드포인트가 등록되지 않습니다.
          """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "테스트 회원 생성 또는 로그인 성공",
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
                "message": "테스트 회원가입에 성공했습니다.",
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
        description = "key 형식 오류",
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
                  "key": "key는 tester-1 ~ tester-99 형식이어야 합니다."
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
                "message": "테스트 회원가입 비밀값이 올바르지 않습니다.",
                "result": {}
              }
              """)))
  })
  @PostMapping("/api/v1/auth/fake-signup")
  ResponseEntity<ApiResponse<SocialLoginResponse>> fakeSignUp(
      @Valid @RequestBody FakeSignUpRequest request);
}
