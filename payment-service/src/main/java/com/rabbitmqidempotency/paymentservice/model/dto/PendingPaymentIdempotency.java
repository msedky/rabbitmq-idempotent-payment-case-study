package com.rabbitmqidempotency.paymentservice.model.dto;

import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PendingPaymentIdempotency {
    private PaymentEntity payment;
    private IdempotencyRecordEntity idempotencyRecord;
}