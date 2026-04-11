package com.rabbitmqidempotency.paymentservice.messaging;

import com.rabbitmqidempotency.paymentservice.config.RabbitMqConstants;
import com.rabbitmqidempotency.paymentservice.messaging.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@SpringJUnitConfig
@ContextConfiguration(classes = PaymentEventPublisherImpl.class)
class PaymentEventPublisherImplTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentEventPublisher publisher;

    @Test
    void publishPaymentCompleted_shouldSendEventToRabbitMq() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .paymentId(UUID.randomUUID())
                .invoiceId("INV-001")
                .customerId("CUST-001")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .providerReference("PSP-123")
                .occurredAt(Instant.now())
                .build();

        publisher.publishPaymentCompleted(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMqConstants.PAYMENT_EXCHANGE,
                RabbitMqConstants.PAYMENT_COMPLETED_ROUTING_KEY,
                event
        );
    }
}