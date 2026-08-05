package com.bookshelves.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.global.exception.ProjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class KakaoTokenVerifierTest {

  private static final String ACCESS_TOKEN_INFO_URI =
      "https://kapi.kakao.com/v1/user/access_token_info";
  private static final String OUR_APP_ID = "1512510";

  private MockRestServiceServer mockServer;
  private KakaoTokenVerifier kakaoTokenVerifier;

  @BeforeEach
  void setUp() {
    RestClient.Builder restClientBuilder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    kakaoTokenVerifier =
        new KakaoTokenVerifier(
            restClientBuilder.baseUrl(ACCESS_TOKEN_INFO_URI).build(), OUR_APP_ID);
  }

  @Test
  void getProviderReturnsKakao() {
    assertThat(kakaoTokenVerifier.getProvider()).isEqualTo(Provider.KAKAO);
  }

  @Test
  void verifyReturnsProviderIdForValidTokenIssuedForOurApp() {
    mockServer
        .expect(requestTo(ACCESS_TOKEN_INFO_URI))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
        .andRespond(
            withSuccess("{\"id\": 123456789, \"app_id\": 1512510}", MediaType.APPLICATION_JSON));

    ProviderUserInfo userInfo = kakaoTokenVerifier.verify("valid-token");

    assertThat(userInfo.providerId()).isEqualTo("123456789");
  }

  @Test
  void verifyThrowsProjectExceptionWhenTokenIssuedForDifferentApp() {
    // 다른 카카오 앱에서 발급된 토큰 — app_id가 우리 앱과 다름
    mockServer
        .expect(requestTo(ACCESS_TOKEN_INFO_URI))
        .andRespond(
            withSuccess("{\"id\": 123456789, \"app_id\": 999999}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> kakaoTokenVerifier.verify("other-app-token"))
        .isInstanceOf(ProjectException.class);
  }

  @Test
  void verifyThrowsProjectExceptionForInvalidToken() {
    mockServer
        .expect(requestTo(ACCESS_TOKEN_INFO_URI))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(() -> kakaoTokenVerifier.verify("invalid-token"))
        .isInstanceOf(ProjectException.class);
  }

  @Test
  void verifyPropagatesOriginalExceptionForKakaoServerError() {
    mockServer
        .expect(requestTo(ACCESS_TOKEN_INFO_URI))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    assertThatThrownBy(() -> kakaoTokenVerifier.verify("any-token"))
        .isInstanceOf(HttpServerErrorException.class)
        .isNotInstanceOf(ProjectException.class);
  }
}
