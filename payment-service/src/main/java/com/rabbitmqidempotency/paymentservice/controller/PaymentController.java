package com.rabbitmqidempotency.paymentservice.controller;

import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return paymentService.create(idempotencyKey, request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPaymentById(@PathVariable UUID paymentId){
        return paymentService.getById(paymentId);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments(){
        return paymentService.getAll();
    }
}