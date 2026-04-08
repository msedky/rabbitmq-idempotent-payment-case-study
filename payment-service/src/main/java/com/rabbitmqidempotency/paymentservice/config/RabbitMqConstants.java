package com.rabbitmqidempotency.paymentservice.config;

public final class RabbitMqConstants {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";

    private RabbitMqConstants() {
    }
}