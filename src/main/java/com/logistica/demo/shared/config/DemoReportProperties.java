package com.logistica.demo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.report")
public record DemoReportProperties(String companyName) {
}
