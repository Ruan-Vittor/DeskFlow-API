package com.deskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deskflow.jwt")
public record JwtProperties(String secret, Long expirationMinutes) {
}
