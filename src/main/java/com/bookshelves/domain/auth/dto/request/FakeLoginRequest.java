package com.bookshelves.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class FakeLoginRequest {

  // 이미 존재하는 회원의 식별자. 이 회원으로 토큰을 발급한다.
  @NotNull
  @Positive
  @Schema(
      description = "토큰을 발급할 기존 회원의 ID",
      example = "10",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private Long memberId;

  // 서버의 FAKE_SIGNUP_SECRET과 일치해야 한다.
  @NotBlank
  @Schema(
      description = "서버의 FAKE_SIGNUP_SECRET과 일치해야 하는 테스트용 비밀값",
      example = "local-test-secret",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String secret;
}
