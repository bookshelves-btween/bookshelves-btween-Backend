package com.bookshelves.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    long accessTokenExpirationSeconds,
    long refreshTokenExpirationSeconds,
    long restoreTokenExpirationSeconds) {}
