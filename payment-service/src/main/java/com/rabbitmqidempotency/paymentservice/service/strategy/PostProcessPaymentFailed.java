package com.rabbitmqidempotency.paymentservice.service.strategy;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.service.PostProcessPaymentStrategy;
import com.rabbitmqidempotency.paymentservice.service.support.PaymentFailureRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostProcessPaymentFailed implements PostProcessPaymentStrategy {

    private final PaymentFailureRecorder paymentFailureRecorder;

    @Override
    public PaymentResponse execute(PspPaymentResponse pspResponse, PaymentEntity payment, IdempotencyRecordEntity idempotencyRecord, Throwable throwable) {
        paymentFailureRecorder.recordFailure(payment, idempotencyRecord, throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        throw new RuntimeException(throwable);
    }
}