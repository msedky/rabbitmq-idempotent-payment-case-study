package com.rabbitmqidempotency.pspmockservice.service;

import com.rabbitmqidempotency.pspmockservice.model.dto.request.PspPaymentRequest;
import com.rabbitmqidempotency.pspmockservice.model.dto.response.PspPaymentResponse;

public interface PspMockService {

    PspPaymentResponse processPayment(PspPaymentRequest request, String scenarioHeader);
}