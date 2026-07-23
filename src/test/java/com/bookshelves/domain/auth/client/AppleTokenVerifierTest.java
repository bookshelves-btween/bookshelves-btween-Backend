package com.bookshelves.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.enums.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AppleTokenVerifierTest {

  private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
  private final OidcIdTokenVerifier oidcIdTokenVerifier = mock(OidcIdTokenVerifier.class);
  private final AppleTokenVerifier appleTokenVerifier =
      new AppleTokenVerifier(jwtDecoder, oidcIdTokenVerifier);

  @Test
  void getProviderReturnsApple() {
    assertThat(appleTokenVerifier.getProvider()).isEqualTo(Provider.APPLE);
  }

  @Test
  void verifyDelegatesToOidcIdTokenVerifierAndWrapsSubject() {
    when(oidcIdTokenVerifier.verifySubject(jwtDecoder, "id-token")).thenReturn("apple-user-id");

    ProviderUserInfo userInfo = appleTokenVerifier.verify("id-token");

    assertThat(userInfo.providerId()).isEqualTo("apple-user-id");
  }
}
