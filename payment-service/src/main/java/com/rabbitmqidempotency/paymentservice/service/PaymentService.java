package com.rabbitmqidempotency.paymentservice.service;

import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse create(String idempotencyKey, CreatePaymentRequest request);

    PaymentResponse getById(UUID paymentId);

    List<PaymentResponse> getAll();
}