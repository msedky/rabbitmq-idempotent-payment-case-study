package com.rabbitmqidempotency.pspmockservice.service.impl;

import com.rabbitmqidempotency.pspmockservice.model.dto.request.PspPaymentRequest;
import com.rabbitmqidempotency.pspmockservice.model.dto.response.PspPaymentResponse;
import com.rabbitmqidempotency.pspmockservice.model.enums.PspScenario;
import com.rabbitmqidempotency.pspmockservice.service.PspMockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PspMockServiceImpl implements PspMockService {

    @Override
    public PspPaymentResponse processPayment(PspPaymentRequest request, String scenarioHeader) {
        PspScenario scenario = resolveScenario(scenarioHeader);

        log.info("Processing PSP payment for invoiceId={} with scenario={}",
                request.getInvoiceId(), scenario);

        switch (scenario) {
            case SUCCESS -> {
                return buildSuccessResponse("Payment processed successfully");
            }
            case DELAYED_SUCCESS -> {
                sleepSeconds(8);
                return buildSuccessResponse("Payment processed successfully after delay");
            }
            case TIMEOUT -> {
                sleepSeconds(20);
                return buildSuccessResponse("Payment processed successfully after long delay");
            }
            default -> throw new IllegalStateException("Unexpected PSP scenario: " + scenario);
        }
    }

    private PspScenario resolveScenario(String scenarioHeader) {
        if (scenarioHeader == null || scenarioHeader.isBlank()) {
            return PspScenario.SUCCESS;
        }

        try {
            return PspScenario.valueOf(scenarioHeader.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown PSP scenario '{}', defaulting to SUCCESS", scenarioHeader);
            return PspScenario.SUCCESS;
        }
    }

    private PspPaymentResponse buildSuccessResponse(String message) {
        return PspPaymentResponse.builder()
                .providerReference("PSP-" + UUID.randomUUID())
                .status("SUCCESS")
                .message(message)
                .processedAt(OffsetDateTime.now())
                .build();
    }

    private void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PSP mock processing was interrupted");
        }
    }
}