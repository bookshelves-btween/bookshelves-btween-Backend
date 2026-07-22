package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.member.enums.Provider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class AppleTokenVerifier implements ProviderTokenVerifier {

  private final JwtDecoder jwtDecoder;
  private final OidcIdTokenVerifier oidcIdTokenVerifier;

  public AppleTokenVerifier(
      @Qualifier("appleJwtDecoder") JwtDecoder jwtDecoder,
      OidcIdTokenVerifier oidcIdTokenVerifier) {
    this.jwtDecoder = jwtDecoder;
    this.oidcIdTokenVerifier = oidcIdTokenVerifier;
  }

  @Override
  public Provider getProvider() {
    return Provider.APPLE;
  }

  @Override
  public ProviderUserInfo verify(String providerToken) {
    String subject = oidcIdTokenVerifier.verifySubject(jwtDecoder, providerToken);
    return new ProviderUserInfo(subject);
  }
}
