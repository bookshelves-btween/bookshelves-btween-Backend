package com.bookshelves.domain.auth.client;

import com.bookshelves.domain.auth.exception.AuthErrorCode;
import com.bookshelves.global.exception.ProjectException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class OidcIdTokenVerifier {

  public String verifySubject(JwtDecoder jwtDecoder, String idToken) {
    try {
      return jwtDecoder.decode(idToken).getSubject();
    } catch (JwtException e) {
      throw new ProjectException(AuthErrorCode.AUTH_INVALID_PROVIDER_TOKEN);
    }
  }
}
