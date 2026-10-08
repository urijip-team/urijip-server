package com.urijip.server.global.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public record JwtProperties(String secret, Duration accessExpiration, Duration refreshExpiration) {
}
