package com.bookshelves.domain.auth.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SocialLoginRequestTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void jacksonDeserializesRequestBodyThroughBuilder() {
    String json = "{\"provider\":\"KAKAO\",\"providerToken\":\"abc\"}";

    SocialLoginRequest request = jsonMapper.readValue(json, SocialLoginRequest.class);

    assertThat(request.getProvider()).isEqualTo("KAKAO");
    assertThat(request.getProviderToken()).isEqualTo("abc");
  }

  @Test
  void jacksonDeserializesUnsupportedProviderValueWithoutFailing() {
    String json = "{\"provider\":\"NAVER\",\"providerToken\":\"abc\"}";

    SocialLoginRequest request = jsonMapper.readValue(json, SocialLoginRequest.class);

    assertThat(request.getProvider()).isEqualTo("NAVER");
  }
}
