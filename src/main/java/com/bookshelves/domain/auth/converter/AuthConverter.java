package com.bookshelves.domain.auth.converter;

import com.bookshelves.domain.auth.dto.response.SocialLoginResponse;
import com.bookshelves.domain.member.enums.MemberStatus;
import java.time.OffsetDateTime;

public class AuthConverter {

  private AuthConverter() {}

  public static SocialLoginResponse toSocialLoginTokenResponse(
      String accessToken,
      String refreshToken,
      long accessTokenExpiresIn,
      long refreshTokenExpiresIn,
      MemberStatus memberStatus) {
    return SocialLoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .accessTokenExpiresIn(accessTokenExpiresIn)
        .refreshTokenExpiresIn(refreshTokenExpiresIn)
        .memberStatus(memberStatus)
        .build();
  }

  public static SocialLoginResponse toSocialLoginWithdrawnResponse(
      String restoreToken, long restoreTokenExpiresIn, OffsetDateTime scheduledDeletionAt) {
    return SocialLoginResponse.builder()
        .restoreToken(restoreToken)
        .restoreTokenExpiresIn(restoreTokenExpiresIn)
        .memberStatus(MemberStatus.WITHDRAWN)
        .scheduledDeletionAt(scheduledDeletionAt)
        .build();
  }
}
