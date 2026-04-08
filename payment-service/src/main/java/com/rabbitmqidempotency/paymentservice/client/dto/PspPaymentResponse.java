package com.rabbitmqidempotency.paymentservice.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PspPaymentResponse {

    private String providerReference;
    private String status;
}