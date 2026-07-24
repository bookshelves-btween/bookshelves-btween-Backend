package com.bookshelves.domain.member.dto.request;

import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class MemberUpdateRequest {

  private String nicknameNoun;
  private String nicknameModifier;
  private String nicknameAnimal;
  private ProfileBackgroundColor profileBackgroundColor;
  private List<Long> categoryIds;
}
