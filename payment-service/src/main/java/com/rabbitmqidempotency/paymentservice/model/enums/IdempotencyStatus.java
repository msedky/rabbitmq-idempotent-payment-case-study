package com.rabbitmqidempotency.paymentservice.model.enums;

public enum IdempotencyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED_RETRYABLE,
    FAILED
}
