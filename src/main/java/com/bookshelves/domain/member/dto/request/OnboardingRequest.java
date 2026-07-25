package com.bookshelves.domain.member.dto.request;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class OnboardingRequest {

  @NotBlank private String nicknameNoun;
  @NotBlank private String nicknameModifier;
  @NotBlank private String nicknameAnimal;
  @NotNull private ProfileBackgroundColor profileBackgroundColor;
  private List<Long> categoryIds;
}
