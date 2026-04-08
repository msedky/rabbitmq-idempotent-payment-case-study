package com.rabbitmqidempotency.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "psp")
public record PspProperties(
        String baseUrl
) {
}