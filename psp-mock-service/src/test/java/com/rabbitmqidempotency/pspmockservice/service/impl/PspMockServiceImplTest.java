package com.rabbitmqidempotency.pspmockservice.service.impl;

import com.rabbitmqidempotency.pspmockservice.model.dto.request.PspPaymentRequest;
import com.rabbitmqidempotency.pspmockservice.model.dto.response.PspPaymentResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PspMockServiceImplTest {

    private final PspMockServiceImpl service = new PspMockServiceImpl();

    private PspPaymentRequest buildRequest() {
        return PspPaymentRequest.builder()
                .invoiceId("INV-123")
                .customerId("CUST-456")
                .amount(BigDecimal.valueOf(100))
                .currency("USD")
                .build();
    }

    @Test
    void testSuccessScenario() {
        PspPaymentResponse response = service.processPayment(buildRequest(), "SUCCESS");

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getProviderReference());
        assertNotNull(response.getProcessedAt());
        assertTrue(response.getMessage().contains("successfully"));
    }

    @Test
    void testDelayedSuccessScenario() {
        PspPaymentResponse response = service.processPayment(buildRequest(), "DELAYED_SUCCESS");

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("after delay"));
    }

    @Test
    void testTimeoutScenario() {
        PspPaymentResponse response = service.processPayment(buildRequest(), "TIMEOUT");

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("long delay"));
    }

    @Test
    void testUnknownScenarioDefaultsToSuccess() {
        PspPaymentResponse response = service.processPayment(buildRequest(), "INVALID");

        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("successfully"));
    }

    @Test
    void testNullScenarioDefaultsToSuccess() {
        PspPaymentResponse response = service.processPayment(buildRequest(), null);

        assertEquals("SUCCESS", response.getStatus());
    }
}
