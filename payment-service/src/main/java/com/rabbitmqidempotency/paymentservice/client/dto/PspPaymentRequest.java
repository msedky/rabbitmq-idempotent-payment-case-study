package com.rabbitmqidempotency.paymentservice.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class PspPaymentRequest {

    private String invoiceId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private String pspScenario;
}