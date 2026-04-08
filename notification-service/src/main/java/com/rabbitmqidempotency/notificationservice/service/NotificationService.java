package com.rabbitmqidempotency.notificationservice.service;

import com.rabbitmqidempotency.notificationservice.model.dto.PaymentCompletedEvent;
import com.rabbitmqidempotency.notificationservice.model.dto.response.NotificationResponseDto;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponseDto getById(UUID id);
    List<NotificationResponseDto> getAll();

    void handlePaymentCompleted(PaymentCompletedEvent event);
}