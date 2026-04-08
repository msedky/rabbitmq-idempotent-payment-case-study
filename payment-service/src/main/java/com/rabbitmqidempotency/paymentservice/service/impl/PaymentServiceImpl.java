package com.rabbitmqidempotency.paymentservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqidempotency.paymentservice.client.PspClient;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentRequest;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.exception.BadRequestException;
import com.rabbitmqidempotency.paymentservice.exception.ConflictException;
import com.rabbitmqidempotency.paymentservice.exception.PaymentNotFoundException;
import com.rabbitmqidempotency.paymentservice.mapper.PaymentMapper;
import com.rabbitmqidempotency.paymentservice.messaging.PaymentEventPublisher;
import com.rabbitmqidempotency.paymentservice.messaging.event.PaymentCompletedEvent;
import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.model.enums.IdempotencyStatus;
import com.rabbitmqidempotency.paymentservice.model.enums.PaymentStatus;
import com.rabbitmqidempotency.paymentservice.repository.IdempotencyRecordRepository;
import com.rabbitmqidempotency.paymentservice.repository.PaymentRepository;
import com.rabbitmqidempotency.paymentservice.service.PaymentService;
import com.rabbitmqidempotency.paymentservice.service.RequestHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final RequestHashService requestHashService;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;
    private final PspClient pspClient;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    @Transactional
    public PaymentResponse create(String idempotencyKey, CreatePaymentRequest request) {
        validateIdempotencyKey(idempotencyKey);

        String requestHash = requestHashService.generateHash(request);

        Optional<IdempotencyRecordEntity> existingRecordOptional =
                idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);

        if (existingRecordOptional.isPresent()) {
            log.info("Idempotency hit for key={}", idempotencyKey);
            return handleExistingIdempotencyRecord(existingRecordOptional.get(), requestHash);
        }

        IdempotencyRecordEntity idempotencyRecord;
        try {
            idempotencyRecord = createInProgressIdempotencyRecord(idempotencyKey, requestHash);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent idempotency insert detected for key={}", idempotencyKey);
            IdempotencyRecordEntity existingRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new ConflictException("Concurrent idempotency conflict occurred"));
            return handleExistingIdempotencyRecord(existingRecord, requestHash);
        }

        log.info("Created idempotency record for key={}", idempotencyKey);

        PaymentEntity payment = createPendingPayment(request);
        log.info("Created pending payment id={} for invoiceId={}", payment.getId(), payment.getInvoiceId());

        try {
            PspPaymentResponse pspResponse = pspClient.processPayment(
                    PspPaymentRequest.builder()
                            .invoiceId(request.getInvoiceId())
                            .customerId(request.getCustomerId())
                            .amount(request.getAmount())
                            .currency(request.getCurrency())
                            .pspScenario(request.getPspScenario())
                            .build()
            );

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
                    idempotencyKey, payment.getId());

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

        } catch (ResourceAccessException ex) {
            log.error("PSP timeout for invoiceId={} and idempotencyKey={}",
                    request.getInvoiceId(), idempotencyKey, ex);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("PSP timeout");
            paymentRepository.save(payment);

            idempotencyRecord.setStatus(IdempotencyStatus.FAILED);
            idempotencyRecord.setResponseHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            idempotencyRecord.setResponseBody(buildFailureResponseBody("PSP timeout"));
            idempotencyRecord.setPaymentId(payment.getId());
            idempotencyRecordRepository.save(idempotencyRecord);

            throw new RuntimeException("PSP timeout", ex);

        } catch (Exception ex) {
            log.error("Payment processing failed for invoiceId={} and idempotencyKey={}: {}",
                    request.getInvoiceId(), idempotencyKey, ex.getMessage(), ex);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);

            idempotencyRecord.setStatus(IdempotencyStatus.FAILED);
            idempotencyRecord.setResponseHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            idempotencyRecord.setResponseBody(buildFailureResponseBody(ex.getMessage()));
            idempotencyRecord.setPaymentId(payment.getId());
            idempotencyRecordRepository.save(idempotencyRecord);

            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with id: " + paymentId
                ));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentMapper.toResponseList(paymentRepository.findAll());
    }


    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
    }

    private PaymentResponse handleExistingIdempotencyRecord(IdempotencyRecordEntity existingRecord, String requestHash) {
        if (!existingRecord.getRequestHash().equals(requestHash)) {
            throw new ConflictException("The provided Idempotency-Key was already used with a different request body");
        }

        if (existingRecord.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new ConflictException("A request with the same Idempotency-Key is already being processed");
        }

        if (existingRecord.getPaymentId() == null) {
            throw new ConflictException("Idempotency record exists but has no associated payment");
        }

        PaymentEntity existingPayment = paymentRepository.findById(existingRecord.getPaymentId())
                .orElseThrow(() -> new ConflictException("Associated payment record was not found"));

        return paymentMapper.toResponse(existingPayment);
    }

    private IdempotencyRecordEntity createInProgressIdempotencyRecord(String idempotencyKey, String requestHash) {
        IdempotencyRecordEntity record = IdempotencyRecordEntity.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .lockedAt(Instant.now())
                .build();

        return idempotencyRecordRepository.save(record);
    }

    private PaymentEntity createPendingPayment(CreatePaymentRequest request) {
        PaymentEntity payment = PaymentEntity.builder()
                .invoiceId(request.getInvoiceId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();

        return paymentRepository.save(payment);
    }

    private String serializeResponse(PaymentResponse paymentResponse) {
        try {
            return objectMapper.writeValueAsString(paymentResponse);
        } catch (JsonProcessingException e) {
            log.error("Serialization failed for paymentResponse={}", paymentResponse, e);
            throw new IllegalStateException("Failed to serialize payment response: " + e.getOriginalMessage(), e);
        }
    }

    private String buildFailureResponseBody(String message) {
        return """
                {
                  "code": "PAYMENT_PROCESSING_FAILED",
                  "message": "%s"
                }
                """.formatted(escapeJson(message));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"");
    }
}