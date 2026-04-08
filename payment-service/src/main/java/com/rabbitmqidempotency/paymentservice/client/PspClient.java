package com.rabbitmqidempotency.paymentservice.client;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentRequest;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;

public interface PspClient {

    PspPaymentResponse processPayment(PspPaymentRequest request);
}