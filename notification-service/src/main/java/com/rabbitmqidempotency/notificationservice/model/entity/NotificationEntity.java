package com.rabbitmqidempotency.notificationservice.model.entity;

import com.rabbitmqidempotency.notificationservice.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "invoice_id", nullable = false, length = 100)
    private String invoiceId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}