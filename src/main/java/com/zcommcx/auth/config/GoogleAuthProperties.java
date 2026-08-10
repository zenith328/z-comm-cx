package com.zcommcx.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google")
public record GoogleAuthProperties(String clientId) {
}
