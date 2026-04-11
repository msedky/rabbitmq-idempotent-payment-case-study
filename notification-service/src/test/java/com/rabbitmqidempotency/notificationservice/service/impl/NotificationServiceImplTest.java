package com.rabbitmqidempotency.notificationservice.service.impl;

import com.rabbitmqidempotency.notificationservice.exception.NotFoundException;
import com.rabbitmqidempotency.notificationservice.mapper.NotificationMapper;
import com.rabbitmqidempotency.notificationservice.model.dto.PaymentCompletedEvent;
import com.rabbitmqidempotency.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqidempotency.notificationservice.model.entity.NotificationEntity;
import com.rabbitmqidempotency.notificationservice.model.enums.NotificationStatus;
import com.rabbitmqidempotency.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void getById_shouldReturnNotification_whenExists() {
        UUID id = UUID.randomUUID();

        NotificationEntity entity = NotificationEntity.builder()
                .id(id)
                .eventId("event-1")
                .invoiceId("INV-1")
                .customerId("CUST-1")
                .message("message")
                .status(NotificationStatus.SENT)
                .build();

        NotificationResponseDto response = NotificationResponseDto.builder()
                .id(id)
                .eventId("event-1")
                .invoiceId("INV-1")
                .customerId("CUST-1")
                .message("message")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findById(id)).thenReturn(Optional.of(entity));
        when(notificationMapper.toResponseDto(entity)).thenReturn(response);

        NotificationResponseDto result = notificationService.getById(id);

        assertEquals(id, result.getId());
        assertEquals("event-1", result.getEventId());
        verify(notificationRepository).findById(id);
        verify(notificationMapper).toResponseDto(entity);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenNotExists() {
        UUID id = UUID.randomUUID();

        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> notificationService.getById(id));

        verify(notificationRepository).findById(id);
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void getAll_shouldReturnMappedNotifications() {
        NotificationEntity entity1 = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .eventId("event-1")
                .invoiceId("INV-1")
                .customerId("CUST-1")
                .message("message-1")
                .status(NotificationStatus.SENT)
                .build();

        NotificationEntity entity2 = NotificationEntity.builder()
                .id(UUID.randomUUID())
                .eventId("event-2")
                .invoiceId("INV-2")
                .customerId("CUST-2")
                .message("message-2")
                .status(NotificationStatus.SENT)
                .build();

        NotificationResponseDto dto1 = NotificationResponseDto.builder()
                .id(entity1.getId())
                .eventId("event-1")
                .invoiceId("INV-1")
                .customerId("CUST-1")
                .message("message-1")
                .status(NotificationStatus.SENT)
                .build();

        NotificationResponseDto dto2 = NotificationResponseDto.builder()
                .id(entity2.getId())
                .eventId("event-2")
                .invoiceId("INV-2")
                .customerId("CUST-2")
                .message("message-2")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findAll()).thenReturn(List.of(entity1, entity2));
        when(notificationMapper.toResponseDto(entity1)).thenReturn(dto1);
        when(notificationMapper.toResponseDto(entity2)).thenReturn(dto2);

        List<NotificationResponseDto> result = notificationService.getAll();

        assertEquals(2, result.size());
        verify(notificationRepository).findAll();
        verify(notificationMapper).toResponseDto(entity1);
        verify(notificationMapper).toResponseDto(entity2);
    }

    @Test
    void getAll_shouldReturnEmptyList_whenNoNotifications() {
        when(notificationRepository.findAll()).thenReturn(List.of());

        List<NotificationResponseDto> result = notificationService.getAll();

        assertTrue(result.isEmpty());
        verify(notificationRepository).findAll();
        verifyNoInteractions(notificationMapper);
    }

    @Test
    void handlePaymentCompleted_shouldPersistNotification() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId("event-123")
                .paymentId(UUID.randomUUID())
                .invoiceId("INV-123")
                .customerId("CUST-123")
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .providerReference("PSP-123")
                .occurredAt(Instant.now())
                .build();

        notificationService.handlePaymentCompleted(event);

        ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(captor.capture());

        NotificationEntity saved = captor.getValue();

        assertEquals(event.getEventId(), saved.getEventId());
        assertEquals(event.getPaymentId(), saved.getPaymentId());
        assertEquals(event.getInvoiceId(), saved.getInvoiceId());
        assertEquals(event.getCustomerId(), saved.getCustomerId());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertTrue(saved.getMessage().contains("invoiceId=" + event.getInvoiceId()));
        assertTrue(saved.getMessage().contains("paymentId=" + event.getPaymentId()));
        assertTrue(saved.getMessage().contains("amount=" + event.getAmount()));
        assertTrue(saved.getMessage().contains(event.getCurrency()));
    }
}