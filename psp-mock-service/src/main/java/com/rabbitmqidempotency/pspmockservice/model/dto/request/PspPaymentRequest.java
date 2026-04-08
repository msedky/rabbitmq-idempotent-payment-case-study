package com.rabbitmqidempotency.pspmockservice.model.dto.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PspPaymentRequest {

    private String invoiceId;
    private String customerId;
    private BigDecimal amount;
    private String currency;
}