package com.rabbitmqidempotency.paymentservice.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @NotBlank(message = "invoiceId is required")
    private String invoiceId;

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.0001", inclusive = true, message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    private String currency;

    private String pspScenario;
}