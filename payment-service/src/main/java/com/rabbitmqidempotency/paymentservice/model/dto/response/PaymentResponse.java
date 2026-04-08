package com.rabbitmqidempotency.paymentservice.model.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID paymentId;
    private String invoiceId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String providerReference;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}