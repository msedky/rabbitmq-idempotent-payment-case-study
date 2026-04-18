package com.rabbitmqidempotency.paymentservice.model.enums;

public enum PaymentStatus {
    PENDING,SUCCESS, FAILED_RETRYABLE,FAILED
}