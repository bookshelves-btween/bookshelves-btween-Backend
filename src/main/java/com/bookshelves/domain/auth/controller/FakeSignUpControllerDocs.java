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
      summary = "테스트용 토큰 발급 (임시)",
      description =
          """
          소셜 로그인 없이 테스트 회원을 만들고 토큰을 발급한다. 시뮬레이터 여러 대에서
          서로 다른 회원으로 모임 참여와 채팅을 확인하기 위한 임시 경로다.

          이 API는 앱이 호출하지 않는다. Swagger에서 직접 호출해 받은 accessToken과
          refreshToken을 각 기기에 넣어 쓴다.

          호출하려면 서버 환경 변수 FAKE_SIGNUP_SECRET과 같은 값을 secret으로 보내야 한다.
          값이 설정되지 않은 환경에서는 모든 요청을 거부한다.

          같은 key로 다시 호출하면 같은 회원의 토큰을 발급하고, 처음 보는 key면 회원을 새로 만든다.
          만들어진 회원은 닉네임까지 채워 ACTIVE 상태로 시작하므로 온보딩을 거치지 않는다.
          key는 tester-1 ~ tester-99 형식만 허용한다.

          임시 API다. prod 프로파일에서는 이 컨트롤러 자체가 등록되지 않는다.
          앱 공개 전에 이 엔드포인트와 관련 설정을 모두 제거한다.
          """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "테스트 회원가입 성공",
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
