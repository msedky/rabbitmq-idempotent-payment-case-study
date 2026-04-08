package com.rabbitmqidempotency.notificationservice.listener;

import com.rabbitmqidempotency.notificationservice.config.RabbitMqConstants;
import com.rabbitmqidempotency.notificationservice.model.dto.PaymentCompletedEvent;
import com.rabbitmqidempotency.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMqConstants.PAYMENT_COMPLETED_QUEUE)
    public void consume(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for paymentId={} and eventId={}",
                event.getPaymentId(), event.getEventId());

        notificationService.handlePaymentCompleted(event);
    }
}