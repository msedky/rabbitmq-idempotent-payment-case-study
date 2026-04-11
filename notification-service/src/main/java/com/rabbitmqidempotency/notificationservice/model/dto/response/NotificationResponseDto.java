package com.rabbitmqidempotency.notificationservice.model.dto.response;

import com.rabbitmqidempotency.notificationservice.model.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponseDto {
    private UUID id;
    private String eventId;
    private UUID paymentId;
    private String invoiceId;
    private String customerId;
    private String message;
    private NotificationStatus status;
    private Instant createdAt;
}
