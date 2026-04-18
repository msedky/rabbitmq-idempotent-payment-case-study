package com.rabbitmqidempotency.paymentservice.client;

import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentRequest;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.config.PspProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PspClientImpl implements PspClient {

    private final RestClient pspRestClient;
    private final PspProperties pspProperties;

    @Override
    @Retryable(
            includes = {ResourceAccessException.class},
            maxRetries = 3,
            delay = 2000,
            multiplier = 2,
            maxDelay = 10000
    )
    public PspPaymentResponse processPayment(PspPaymentRequest request) {
        log.info("calling PSP with invoiceId: {}, customerId: {}", request.getInvoiceId(), request.getCustomerId());
        return pspRestClient.post()
                .uri(pspProperties.baseUrl() + "/api/v1/psp/payments")
                .header("X-PSP-Scenario", request.getPspScenario() == null ? "SUCCESS" : request.getPspScenario())
                .body(request)
                .retrieve()
                .body(PspPaymentResponse.class);
    }
}