package com.bookshelves.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth2.google")
public record GoogleOidcProperties(String clientId, String jwkSetUri, String issuer) {}
