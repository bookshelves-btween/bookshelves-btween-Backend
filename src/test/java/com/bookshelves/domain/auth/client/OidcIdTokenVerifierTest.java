package com.bookshelves.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookshelves.global.exception.ProjectException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class OidcIdTokenVerifierTest {

  private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
  private final OidcIdTokenVerifier oidcIdTokenVerifier = new OidcIdTokenVerifier();

  @Test
  void verifySubjectReturnsSubjectClaimForValidToken() {
    Jwt jwt =
        Jwt.withTokenValue("id-token")
            .header("alg", "none")
            .claim("sub", "provider-user-id")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    when(jwtDecoder.decode("id-token")).thenReturn(jwt);

    String subject = oidcIdTokenVerifier.verifySubject(jwtDecoder, "id-token");

    assertThat(subject).isEqualTo("provider-user-id");
  }

  @Test
  void verifySubjectThrowsProjectExceptionForInvalidToken() {
    when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("invalid"));

    assertThatThrownBy(() -> oidcIdTokenVerifier.verifySubject(jwtDecoder, "invalid-token"))
        .isInstanceOf(ProjectException.class);
  }
}
