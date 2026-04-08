package com.rabbitmqidempotency.pspmockservice.controller;

import com.rabbitmqidempotency.pspmockservice.model.dto.request.PspPaymentRequest;
import com.rabbitmqidempotency.pspmockservice.model.dto.response.PspPaymentResponse;
import com.rabbitmqidempotency.pspmockservice.service.PspMockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/psp/payments")
@RequiredArgsConstructor
public class PspMockController {

    private final PspMockService pspMockService;

    @PostMapping
    public PspPaymentResponse processPayment(
            @RequestHeader(name = "X-PSP-Scenario", required = false) String scenarioHeader,
            @RequestBody PspPaymentRequest request
    ) {
        return pspMockService.processPayment(request, scenarioHeader);
    }
}