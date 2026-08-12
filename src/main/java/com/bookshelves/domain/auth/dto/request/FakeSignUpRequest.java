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

  // 회원을 구분하는 값이다. 같은 key로 다시 호출하면 같은 회원으로 로그인하고,
  // 처음 보는 key면 새 회원을 만든다. 소셜 로그인의 providerId 자리에 대응한다.
  //
  // 형식을 tester-1 ~ tester-99로 묶어 만들어질 수 있는 테스트 회원 수를 제한한다.
  // 임의 문자열을 허용하면 아무 값이나 계정과 닉네임으로 남는다.
  @NotBlank
  @Pattern(regexp = "^tester-[1-9][0-9]?$", message = "key는 tester-1 ~ tester-99 형식이어야 합니다.")
  @Schema(
      description = "테스트 회원 식별 키. 동일한 키를 다시 사용하면 같은 회원으로 로그인합니다.",
      example = "tester-1",
      pattern = "^tester-[1-9][0-9]?$",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String key;

  // 서버 환경 변수 FAKE_SIGNUP_SECRET과 일치해야 한다. 저장소가 공개되어 있어 코드에 값을 둘 수 없다.
  @NotBlank
  @Schema(
      description = "서버의 FAKE_SIGNUP_SECRET과 일치해야 하는 테스트용 비밀값",
      example = "local-test-secret",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String secret;
}
