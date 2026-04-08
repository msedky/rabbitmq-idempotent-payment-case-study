package com.rabbitmqidempotency.paymentservice.messaging;

import com.rabbitmqidempotency.paymentservice.messaging.event.PaymentCompletedEvent;

public interface PaymentEventPublisher {

    void publishPaymentCompleted(PaymentCompletedEvent event);
}