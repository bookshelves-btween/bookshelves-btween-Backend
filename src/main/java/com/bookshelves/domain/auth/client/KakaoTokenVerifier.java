package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.global.exception.ProjectException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoTokenVerifier implements ProviderTokenVerifier {

  private final RestClient restClient;

  public KakaoTokenVerifier(
      RestClient.Builder restClientBuilder,
      @Value("${external.kakao.user-info-uri}") String userInfoUri) {
    this.restClient = restClientBuilder.baseUrl(userInfoUri).build();
  }

  @Override
  public Provider getProvider() {
    return Provider.KAKAO;
  }

  @Override
  public ProviderUserInfo verify(String providerToken) {
    KakaoUserResponse response;

    try {
      response =
          restClient
              .get()
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
              .retrieve()
              .body(KakaoUserResponse.class);
    } catch (RestClientException e) {
      throw new ProjectException(AuthErrorCode.INVALID_PROVIDER_TOKEN);
    }

    if (response == null || response.id() == null) {
      throw new ProjectException(AuthErrorCode.INVALID_PROVIDER_TOKEN);
    }

    return new ProviderUserInfo(String.valueOf(response.id()));
  }

  private record KakaoUserResponse(Long id) {}
}
