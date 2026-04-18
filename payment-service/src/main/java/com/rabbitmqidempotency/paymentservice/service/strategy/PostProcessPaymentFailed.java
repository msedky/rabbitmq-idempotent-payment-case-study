package com.rabbitmqidempotency.paymentservice.service.strategy;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.mapper.PaymentMapper;
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
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse execute(PspPaymentResponse pspResponse, PaymentEntity payment, IdempotencyRecordEntity idempotencyRecord, Throwable throwable) {
        paymentFailureRecorder.recordFailure(payment, idempotencyRecord, throwable != null ? throwable.getMessage() : "Payment Failed", HttpStatus.INTERNAL_SERVER_ERROR.value());
        PaymentResponse paymentResponse = paymentMapper.toResponse(payment);
        return paymentResponse;
    }
}