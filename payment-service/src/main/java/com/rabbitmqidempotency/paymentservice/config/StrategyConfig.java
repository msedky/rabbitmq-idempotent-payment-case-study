package com.rabbitmqidempotency.paymentservice.config;

import com.rabbitmqidempotency.paymentservice.model.enums.PspStatus;
import com.rabbitmqidempotency.paymentservice.service.PostProcessPaymentStrategy;
import com.rabbitmqidempotency.paymentservice.service.strategy.PostProcessPaymentFailed;
import com.rabbitmqidempotency.paymentservice.service.strategy.PostProcessPaymentSuccess;
import com.rabbitmqidempotency.paymentservice.service.strategy.PostProcessPaymentTimeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class StrategyConfig {

    @Bean
    public Map<PspStatus, PostProcessPaymentStrategy> postProcessPaymentStrategyMap(
            PostProcessPaymentSuccess postProcessPaymentSuccess,
            PostProcessPaymentTimeout postProcessPaymentTimeout,
            PostProcessPaymentFailed postProcessPaymentFailed

    ) {
        return Map.of(
                PspStatus.SUCCESS, postProcessPaymentSuccess,
                PspStatus.TIMEOUT, postProcessPaymentTimeout,
                PspStatus.FAILED, postProcessPaymentFailed
        );
    }
}