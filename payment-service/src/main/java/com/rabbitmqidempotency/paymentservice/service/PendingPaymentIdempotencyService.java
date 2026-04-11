package com.rabbitmqidempotency.paymentservice.service;

import com.rabbitmqidempotency.paymentservice.model.dto.PendingPaymentIdempotency;
import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.model.enums.IdempotencyStatus;
import com.rabbitmqidempotency.paymentservice.model.enums.PaymentStatus;
import com.rabbitmqidempotency.paymentservice.repository.IdempotencyRecordRepository;
import com.rabbitmqidempotency.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentIdempotencyService {


    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Transactional
    public PendingPaymentIdempotency createPending(String idempotencyKey, String requestHash, CreatePaymentRequest request) {

        IdempotencyRecordEntity idempotencyRecord = idempotencyRecordRepository.save(IdempotencyRecordEntity.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .lockedAt(Instant.now())
                .build());

        log.info("Created idempotency record for key={}", idempotencyKey);

        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .invoiceId(request.getInvoiceId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build());
        log.info("Saved payment with invoiceId: {} and customerId: {} as PENDING payment", request.getInvoiceId(), request.getCustomerId());

        return PendingPaymentIdempotency.builder()
                .payment(payment)
                .idempotencyRecord(idempotencyRecord)
                .build();
    }
}
