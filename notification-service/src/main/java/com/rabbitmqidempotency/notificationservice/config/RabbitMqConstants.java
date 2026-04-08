package com.rabbitmqidempotency.notificationservice.config;

public final class RabbitMqConstants {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.notification.queue";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";

    private RabbitMqConstants() {
    }
}