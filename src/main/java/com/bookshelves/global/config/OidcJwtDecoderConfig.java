package com.bookshelves.global.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties({GoogleOidcProperties.class, AppleOidcProperties.class})
public class OidcJwtDecoderConfig {

  @Bean
  public JwtDecoder googleJwtDecoder(GoogleOidcProperties properties) {
    return buildJwtDecoder(properties.jwkSetUri(), properties.issuer(), properties.clientId());
  }

  @Bean
  public JwtDecoder appleJwtDecoder(AppleOidcProperties properties) {
    return buildJwtDecoder(properties.jwkSetUri(), properties.issuer(), properties.clientId());
  }

  private JwtDecoder buildJwtDecoder(String jwkSetUri, String issuer, String clientId) {
    NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
    OAuth2TokenValidator<Jwt> withAudience =
        new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD, audience -> audience != null && audience.contains(clientId));

    jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
    return jwtDecoder;
  }
}
