package com.rabbitmqidempotency.notificationservice.controller;

import com.rabbitmqidempotency.notificationservice.model.entity.NotificationEntity;
import com.rabbitmqidempotency.notificationservice.model.enums.NotificationStatus;
import com.rabbitmqidempotency.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationControllerIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("notifications_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;


    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void getAll_shouldReturnEmptyList_whenNoNotificationsExist() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAll_shouldReturnAllNotifications() throws Exception {
        NotificationEntity notification1 = notificationRepository.save(
                NotificationEntity.builder()
                        .eventId("event-1")
                        .paymentId(UUID.randomUUID())
                        .invoiceId("INV-001")
                        .customerId("CUST-001")
                        .message("Payment completed successfully for invoiceId=INV-001, paymentId=pid-1, amount=500.00 USD")
                        .status(NotificationStatus.SENT)
                        .build()
        );

        NotificationEntity notification2 = notificationRepository.save(
                NotificationEntity.builder()
                        .eventId("event-2")
                        .paymentId(UUID.randomUUID())
                        .invoiceId("INV-002")
                        .customerId("CUST-002")
                        .message("Payment completed successfully for invoiceId=INV-002, paymentId=pid-2, amount=700.00 USD")
                        .status(NotificationStatus.SENT)
                        .build()
        );

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].eventId").value(notification1.getEventId()))
                .andExpect(jsonPath("$[0].invoiceId").value(notification1.getInvoiceId()))
                .andExpect(jsonPath("$[0].customerId").value(notification1.getCustomerId()))
                .andExpect(jsonPath("$[0].message").value(notification1.getMessage()))
                .andExpect(jsonPath("$[0].status").value(notification1.getStatus().name()))
                .andExpect(jsonPath("$[1].id").exists())
                .andExpect(jsonPath("$[1].eventId").value(notification2.getEventId()))
                .andExpect(jsonPath("$[1].invoiceId").value(notification2.getInvoiceId()))
                .andExpect(jsonPath("$[1].customerId").value(notification2.getCustomerId()))
                .andExpect(jsonPath("$[1].message").value(notification2.getMessage()))
                .andExpect(jsonPath("$[1].status").value(notification2.getStatus().name()));

        assertEquals(2, notificationRepository.count());
    }

    @Test
    void getById_shouldReturnNotification_whenNotificationExists() throws Exception {
        NotificationEntity savedNotification = notificationRepository.save(
                NotificationEntity.builder()
                        .eventId("event-123")
                        .paymentId(UUID.randomUUID())
                        .invoiceId("INV-123")
                        .customerId("CUST-123")
                        .message("Payment completed successfully for invoiceId=INV-123, paymentId=pid-123, amount=900.00 USD")
                        .status(NotificationStatus.SENT)
                        .build()
        );

        mockMvc.perform(get("/api/v1/notifications/{id}", savedNotification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedNotification.getId().toString()))
                .andExpect(jsonPath("$.eventId").value(savedNotification.getEventId()))
                .andExpect(jsonPath("$.invoiceId").value(savedNotification.getInvoiceId()))
                .andExpect(jsonPath("$.customerId").value(savedNotification.getCustomerId()))
                .andExpect(jsonPath("$.message").value(savedNotification.getMessage()))
                .andExpect(jsonPath("$.status").value(savedNotification.getStatus().name()));
    }

    @Test
    void getById_shouldReturnNotFound_whenNotificationDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
