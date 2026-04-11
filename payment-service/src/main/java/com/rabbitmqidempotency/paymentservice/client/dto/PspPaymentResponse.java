package com.rabbitmqidempotency.paymentservice.client.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class PspPaymentResponse {

    private String providerReference;
    private String status;
}