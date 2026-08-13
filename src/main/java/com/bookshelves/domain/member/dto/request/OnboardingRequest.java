package com.bookshelves.domain.member.dto.request;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @NotBlank
  @Schema(example = "책")
  private String nicknameNoun;

  @NotBlank
  @Schema(example = "먹는")
  private String nicknameModifier;

  @NotBlank
  @Schema(example = "여우")
  private String nicknameAnimal;

  @NotNull private ProfileBackgroundColor profileBackgroundColor;

  @Schema(example = "[1, 2]")
  private List<Long> categoryIds;

  @Schema(example = "[1, 2]")
  private List<Long> agreedTermsIds;
}
