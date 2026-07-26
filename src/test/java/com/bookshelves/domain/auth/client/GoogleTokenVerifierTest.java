package com.bookshelves.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.enums.Provider;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class GoogleTokenVerifierTest {

  private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
  private final OidcIdTokenVerifier oidcIdTokenVerifier = mock(OidcIdTokenVerifier.class);
  private final GoogleTokenVerifier googleTokenVerifier =
      new GoogleTokenVerifier(jwtDecoder, oidcIdTokenVerifier);

  @Test
  void getProviderReturnsGoogle() {
    assertThat(googleTokenVerifier.getProvider()).isEqualTo(Provider.GOOGLE);
  }

  @Test
  void verifyDelegatesToOidcIdTokenVerifierAndWrapsSubject() {
    when(oidcIdTokenVerifier.verifySubject(jwtDecoder, "id-token")).thenReturn("google-user-id");

    ProviderUserInfo userInfo = googleTokenVerifier.verify("id-token");

    assertThat(userInfo.providerId()).isEqualTo("google-user-id");
  }
}
