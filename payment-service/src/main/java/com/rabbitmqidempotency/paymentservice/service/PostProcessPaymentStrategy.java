package com.rabbitmqidempotency.paymentservice.service;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;


public interface PostProcessPaymentStrategy {

    PaymentResponse execute(PspPaymentResponse pspResponse, PaymentEntity payment, IdempotencyRecordEntity idempotencyRecord, Throwable throwable);

}