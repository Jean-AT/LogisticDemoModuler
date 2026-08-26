package com.logistica.demo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.jwt")
public record DemoJwtProperties(String secret, long expirationMs) {
}
