package com.bookshelves.domain.auth.dto.request;

import com.bookshelves.domain.member.enums.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class SocialLoginRequest {

  @NotNull private Provider provider;

  @NotBlank private String providerToken;
}
