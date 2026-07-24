package com.bookshelves.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReissueResponse {

  private String accessToken;
  private String refreshToken;
  private Long accessTokenExpiresIn;
  private Long refreshTokenExpiresIn;
}
