package com.bookshelves.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class FakeSignUpRequest {

  // 같은 key는 같은 테스트 회원을 식별하며 허용 형식으로 계정 수를 제한한다.
  @NotBlank
  @Pattern(regexp = "^tester-[1-9][0-9]?$", message = "key는 tester-1 ~ tester-99 형식이어야 합니다.")
  @Schema(description = "테스트 회원을 구분하는 값", example = "tester-1")
  private String key;

  // 서버의 FAKE_SIGNUP_SECRET과 일치해야 한다.
  @NotBlank
  @Schema(description = "서버에 설정된 테스트 회원가입 비밀값")
  private String secret;
}
