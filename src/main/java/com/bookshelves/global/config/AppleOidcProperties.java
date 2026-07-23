package com.bookshelves.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth2.apple")
public record AppleOidcProperties(String clientId, String jwkSetUri, String issuer) {}
