package com.rabbitmqidempotency.paymentservice.model.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private String code;
    private String message;
    private OffsetDateTime timestamp;
}