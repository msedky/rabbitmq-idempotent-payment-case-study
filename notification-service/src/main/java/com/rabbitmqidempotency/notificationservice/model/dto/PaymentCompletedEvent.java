package com.rabbitmqidempotency.notificationservice.model.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    private String eventId;
    private UUID paymentId;
    private String invoiceId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String providerReference;
    private Instant occurredAt;
}