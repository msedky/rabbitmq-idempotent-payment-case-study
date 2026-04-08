package com.rabbitmqidempotency.paymentservice.messaging;

import com.rabbitmqidempotency.paymentservice.config.RabbitMqConstants;
import com.rabbitmqidempotency.paymentservice.messaging.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisherImpl implements PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.PAYMENT_EXCHANGE,
                RabbitMqConstants.PAYMENT_COMPLETED_ROUTING_KEY,
                event
        );

        log.info("Published PaymentCompletedEvent for paymentId={}", event.getPaymentId());
    }
}