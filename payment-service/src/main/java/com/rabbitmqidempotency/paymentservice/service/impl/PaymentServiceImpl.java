package com.rabbitmqidempotency.paymentservice.service.impl;

import com.rabbitmqidempotency.paymentservice.client.PspClient;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentRequest;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.exception.BadRequestException;
import com.rabbitmqidempotency.paymentservice.exception.ConflictException;
import com.rabbitmqidempotency.paymentservice.exception.PaymentNotFoundException;
import com.rabbitmqidempotency.paymentservice.mapper.PaymentMapper;
import com.rabbitmqidempotency.paymentservice.model.dto.PendingPaymentIdempotency;
import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.model.enums.IdempotencyStatus;
import com.rabbitmqidempotency.paymentservice.model.enums.PspStatus;
import com.rabbitmqidempotency.paymentservice.repository.IdempotencyRecordRepository;
import com.rabbitmqidempotency.paymentservice.repository.PaymentRepository;
import com.rabbitmqidempotency.paymentservice.service.PaymentService;
import com.rabbitmqidempotency.paymentservice.service.PendingPaymentIdempotencyService;
import com.rabbitmqidempotency.paymentservice.service.PostProcessPaymentStrategy;
import com.rabbitmqidempotency.paymentservice.service.RequestHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    private final PaymentMapper paymentMapper;

    private final RequestHashService requestHashService;

    private final PendingPaymentIdempotencyService pendingPaymentIdempotencyService;

    private final PspClient pspClient;

    private final Map<PspStatus, PostProcessPaymentStrategy> postProcessPaymentStrategyMap;

    @Override
    public PaymentResponse create(String idempotencyKey, CreatePaymentRequest request) {
        validateIdempotencyKey(idempotencyKey);
        String requestHash = requestHashService.generateHash(request);

        PendingPaymentIdempotency pendingPaymentIdempotency;
        try {
            pendingPaymentIdempotency = pendingPaymentIdempotencyService.createPending(idempotencyKey, requestHash, request);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent idempotency insert detected for key={}", idempotencyKey);
            IdempotencyRecordEntity existingRecord = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).get();

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

        PaymentEntity payment = pendingPaymentIdempotency.getPayment();
        IdempotencyRecordEntity idempotencyRecord = pendingPaymentIdempotency.getIdempotencyRecord();

        Exception exception = null;
        PspStatus pspStatus;
        PspPaymentResponse pspResponse = null;

        try {
            pspResponse = pspClient.processPayment(
                    PspPaymentRequest.builder()
                            .invoiceId(request.getInvoiceId())
                            .customerId(request.getCustomerId())
                            .amount(request.getAmount())
                            .currency(request.getCurrency())
                            .pspScenario(request.getPspScenario())
                            .build()
            );
            pspStatus = PspStatus.SUCCESS;

        } catch (ResourceAccessException ex) {
            exception = ex;
            pspStatus = PspStatus.TIMEOUT;
        } catch (Exception ex) {
            exception = ex;
            pspStatus = PspStatus.FAILED;
        }

        return postProcessPaymentStrategyMap.get(pspStatus)
                .execute(pspResponse, payment, idempotencyRecord, exception);
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
}