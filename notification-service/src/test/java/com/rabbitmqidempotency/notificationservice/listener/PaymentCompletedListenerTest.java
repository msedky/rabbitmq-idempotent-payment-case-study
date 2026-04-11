package com.rabbitmqidempotency.notificationservice.listener;

import com.rabbitmqidempotency.notificationservice.model.dto.PaymentCompletedEvent;
import com.rabbitmqidempotency.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentCompletedListener listener;

    @Test
    void consume_shouldDelegateToNotificationService() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId("event-1")
                .paymentId(UUID.randomUUID())
                .invoiceId("INV-1")
                .customerId("CUST-1")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .occurredAt(Instant.now())
                .build();

        listener.consume(event);

        verify(notificationService).handlePaymentCompleted(event);
    }
}