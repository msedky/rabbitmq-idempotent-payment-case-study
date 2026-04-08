package com.rabbitmqidempotency.pspmockservice.model.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PspPaymentResponse {

    private String providerReference;
    private String status;
    private String message;
    private OffsetDateTime processedAt;
}