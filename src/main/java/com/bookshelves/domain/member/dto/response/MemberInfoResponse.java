package com.bookshelves.domain.member.dto.response;

import com.bookshelves.domain.member.enums.MemberStatus;
import com.bookshelves.domain.member.enums.ProfileBackgroundColor;
import com.bookshelves.domain.member.enums.Provider;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoResponse {

  private Long id;
  private String nickname;
  private String nicknameNoun;
  private String nicknameModifier;
  private String nicknameAnimal;
  private ProfileBackgroundColor profileBackgroundColor;
  private Provider provider;
  private MemberStatus memberStatus;
  private LocalDateTime createdAt;
  private List<CategoryResponse> categories;
}
