package com.rabbitmqidempotency.notificationservice.service.impl;

import com.rabbitmqidempotency.notificationservice.exception.NotFoundException;
import com.rabbitmqidempotency.notificationservice.mapper.NotificationMapper;
import com.rabbitmqidempotency.notificationservice.model.dto.PaymentCompletedEvent;
import com.rabbitmqidempotency.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqidempotency.notificationservice.model.entity.NotificationEntity;
import com.rabbitmqidempotency.notificationservice.model.enums.NotificationStatus;
import com.rabbitmqidempotency.notificationservice.repository.NotificationRepository;
import com.rabbitmqidempotency.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponseDto getById(UUID id) {
        log.info("getting Notification By Id" + id);
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + id));
        return notificationMapper.toResponseDto(notification);
    }

    @Override
    public List<NotificationResponseDto> getAll() {
        log.info("getting All Notifications");
        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String message = """
                Payment completed successfully for invoiceId=%s, paymentId=%s, amount=%s %s
                """.formatted(
                event.getInvoiceId(),
                event.getPaymentId(),
                event.getAmount(),
                event.getCurrency()
        ).trim();

        NotificationEntity notification = NotificationEntity.builder()
                .eventId(event.getEventId())
                .paymentId(event.getPaymentId())
                .invoiceId(event.getInvoiceId())
                .customerId(event.getCustomerId())
                .message(message)
                .status(NotificationStatus.SENT)
                .build();

        notificationRepository.save(notification);

        log.info("Notification persisted for paymentId={} and eventId={}",
                event.getPaymentId(), event.getEventId());
    }
}