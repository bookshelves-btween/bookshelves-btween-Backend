package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.domain.auth.exception.AuthException;
import com.bookshelves.domain.member.enums.Provider;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class KakaoTokenVerifier implements ProviderTokenVerifier {

  private final RestClient restClient;

  @Autowired
  public KakaoTokenVerifier(
      RestClient.Builder restClientBuilder,
      @Value("${external.kakao.user-info-uri}") String userInfoUri) {
    this(buildRestClient(restClientBuilder, userInfoUri));
  }

  KakaoTokenVerifier(RestClient restClient) {
    this.restClient = restClient;
  }

  private static RestClient buildRestClient(
      RestClient.Builder restClientBuilder, String userInfoUri) {
    // 로그인 흐름을 직접 막는 동기 호출이라, 응답 지연 시 빠르게 실패하도록 짧게 잡는다.
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(5));

    // restClientBuilder는 싱글톤 빈이라 clone() 없이 바로 설정하면 다른 클라이언트와 상태가 섞인다.
    return restClientBuilder.clone().baseUrl(userInfoUri).requestFactory(requestFactory).build();
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
    } catch (HttpClientErrorException e) {
      // 카카오가 4xx로 요청을 거부한 경우 = 토큰 자체가 유효하지 않음.
      // 5xx/연결실패/응답변환실패 등은 토큰 문제가 아니므로 여기서 잡지 않고 그대로 전파한다.
      throw new AuthException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }

    if (response == null || response.id() == null) {
      throw new AuthException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }

    return new ProviderUserInfo(String.valueOf(response.id()));
  }

  private record KakaoUserResponse(Long id) {}
}
