package com.bookshelves.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SocialLoginRequest {

  // enum으로 받으면 지원하지 않는 값이 Jackson 역직렬화 단계에서 실패해
  // AuthErrorCode.AUTH_UNSUPPORTED_PROVIDER(AUTH400) 대신 범용 400으로 응답되므로
  // String으로 받아 서비스 레이어에서 직접 판별한다.
  @NotBlank
  @Schema(allowableValues = {"KAKAO", "GOOGLE", "APPLE"})
  private String provider;

  @NotBlank private String providerToken;
}
