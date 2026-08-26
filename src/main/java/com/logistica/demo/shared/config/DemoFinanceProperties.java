package com.logistica.demo.shared.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.finance")
public record DemoFinanceProperties(BigDecimal igvRate, BigDecimal exchangeRate) {
}
