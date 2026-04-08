package com.rabbitmqidempotency.paymentservice.client;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentRequest;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.config.PspProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PspClientImpl implements PspClient {

    private final RestClient pspRestClient;
    private final PspProperties pspProperties;

    @Override
    public PspPaymentResponse processPayment(PspPaymentRequest request) {
        return pspRestClient.post()
                .uri(pspProperties.baseUrl() + "/api/v1/psp/payments")
                .header("X-PSP-Scenario", request.getPspScenario() == null ? "SUCCESS" : request.getPspScenario())
                .body(request)
                .retrieve()
                .body(PspPaymentResponse.class);
    }
}