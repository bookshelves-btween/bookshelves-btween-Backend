package com.bookshelves.domain.member.dto.request;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class MemberUpdateRequest {

  @Schema(example = "책")
  private String nicknameNoun;

  @Schema(example = "먹는")
  private String nicknameModifier;

  @Schema(example = "여우")
  private String nicknameAnimal;

  private ProfileBackgroundColor profileBackgroundColor;

  @Schema(example = "[1, 2]")
  private List<Long> categoryIds;
}
