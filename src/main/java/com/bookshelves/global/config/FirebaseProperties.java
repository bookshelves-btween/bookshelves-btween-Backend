package com.bookshelves.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.firebase")
public record FirebaseProperties(boolean enabled, String projectId) {}
