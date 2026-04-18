package com.rabbitmqidempotency.paymentservice.service.strategy;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.mapper.PaymentMapper;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.model.enums.IdempotencyStatus;
import com.rabbitmqidempotency.paymentservice.model.enums.PaymentStatus;
import com.rabbitmqidempotency.paymentservice.repository.IdempotencyRecordRepository;
import com.rabbitmqidempotency.paymentservice.repository.PaymentRepository;
import com.rabbitmqidempotency.paymentservice.service.PostProcessPaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostProcessPaymentTimeout implements PostProcessPaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final Function<String, String> buildFailureResponseBodyHelper;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse execute(PspPaymentResponse pspResponse, PaymentEntity payment, IdempotencyRecordEntity idempotencyRecord, Throwable throwable) {
        log.error("PSP timeout for invoiceId={} and idempotencyKey={}",
                payment.getInvoiceId(), idempotencyRecord.getIdempotencyKey(), throwable);

        payment.setStatus(PaymentStatus.FAILED_RETRYABLE);
        payment.setFailureReason("PSP timeout");
        paymentRepository.save(payment);

        idempotencyRecord.setStatus(IdempotencyStatus.FAILED_RETRYABLE);
        idempotencyRecord.setResponseHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        idempotencyRecord.setResponseBody(buildFailureResponseBodyHelper.apply("PSP timeout"));
        idempotencyRecord.setPaymentId(payment.getId());
        idempotencyRecordRepository.save(idempotencyRecord);

        PaymentResponse paymentResponse = paymentMapper.toResponse(payment);

        return paymentResponse;
    }
}
