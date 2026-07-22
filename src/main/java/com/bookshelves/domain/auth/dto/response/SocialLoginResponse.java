package com.bookshelves.domain.auth.dto.response;

import com.bookshelves.domain.member.enums.MemberStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialLoginResponse {

  private String accessToken;
  private String refreshToken;
  private Long accessTokenExpiresIn;
  private Long refreshTokenExpiresIn;
  private String restoreToken;
  private Long restoreTokenExpiresIn;
  private MemberStatus memberStatus;
  private OffsetDateTime scheduledDeletionAt;
}
