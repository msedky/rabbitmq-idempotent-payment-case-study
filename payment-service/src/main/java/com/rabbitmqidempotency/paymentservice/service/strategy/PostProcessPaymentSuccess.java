package com.rabbitmqidempotency.paymentservice.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.mapper.PaymentMapper;
import com.rabbitmqidempotency.paymentservice.messaging.PaymentEventPublisher;
import com.rabbitmqidempotency.paymentservice.messaging.event.PaymentCompletedEvent;
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

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostProcessPaymentSuccess implements PostProcessPaymentStrategy {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    @Override
    public PaymentResponse execute(PspPaymentResponse pspResponse, PaymentEntity payment, IdempotencyRecordEntity idempotencyRecord, Throwable throwable) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderReference(pspResponse.getProviderReference());
        payment.setFailureReason(null);
        payment = paymentRepository.save(payment);

        log.info("Payment id={} marked SUCCESS with providerReference={}",
                payment.getId(), payment.getProviderReference());

        PaymentResponse paymentResponse = paymentMapper.toResponse(payment);

        idempotencyRecord.setStatus(IdempotencyStatus.COMPLETED);
        idempotencyRecord.setResponseHttpStatus(HttpStatus.CREATED.value());
        idempotencyRecord.setResponseBody(serializeResponse(paymentResponse));
        idempotencyRecord.setPaymentId(payment.getId());
        idempotencyRecordRepository.save(idempotencyRecord);

        log.info("Idempotency record completed for key={} and paymentId={}",
                idempotencyRecord.getIdempotencyKey(), payment.getId());

        paymentEventPublisher.publishPaymentCompleted(
                PaymentCompletedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .paymentId(payment.getId())
                        .invoiceId(payment.getInvoiceId())
                        .customerId(payment.getCustomerId())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .providerReference(payment.getProviderReference())
                        .occurredAt(Instant.now())
                        .build()
        );
        return paymentResponse;
    }

    private String serializeResponse(PaymentResponse paymentResponse) {
        try {
            return objectMapper.writeValueAsString(paymentResponse);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed for paymentResponse={}", paymentResponse, e);
            throw new IllegalStateException("Failed to serialize payment response: " + e.getOriginalMessage(), e);
        }
    }
}
