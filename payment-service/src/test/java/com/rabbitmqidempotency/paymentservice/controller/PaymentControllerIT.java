package com.rabbitmqidempotency.paymentservice.controller;

import com.rabbitmqidempotency.paymentservice.client.PspClient;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentRequest;
import com.rabbitmqidempotency.paymentservice.client.dto.PspPaymentResponse;
import com.rabbitmqidempotency.paymentservice.messaging.PaymentEventPublisher;
import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.model.enums.IdempotencyStatus;
import com.rabbitmqidempotency.paymentservice.model.enums.PaymentStatus;
import com.rabbitmqidempotency.paymentservice.repository.IdempotencyRecordRepository;
import com.rabbitmqidempotency.paymentservice.repository.PaymentRepository;
import com.rabbitmqidempotency.paymentservice.service.RequestHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentControllerIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("payments_test_db")
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
    private JsonMapper objectMapper;

    @Autowired
    private RequestHashService requestHashService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @MockitoBean
    private PspClient pspClient;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @BeforeEach
    void setUp() {
        idempotencyRecordRepository.deleteAll();
        paymentRepository.deleteAll();
        doNothing().when(paymentEventPublisher).publishPaymentCompleted(any());
    }


    @Test
    void create_shouldCreateNewPayment_whenPspSuccess() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");

        PspPaymentResponse pspPaymentResponse = PspPaymentResponse.builder()
                .providerReference("PSP-80c49a46-8d6a-42f6-ac33-130c8b28d760")
                .status("SUCCESS")
                .build();

        when(pspClient.processPayment(
                PspPaymentRequest.builder()
                        .invoiceId(request.getInvoiceId())
                        .customerId(request.getCustomerId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .pspScenario(request.getPspScenario())
                        .build()
        )).thenReturn(pspPaymentResponse);

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "SOME_KEY")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.invoiceId").value(request.getInvoiceId().toString()))
                .andExpect(jsonPath("$.customerId").value(request.getCustomerId()))
                .andExpect(jsonPath("$.amount").value(request.getAmount().doubleValue()))
                .andExpect(jsonPath("$.currency").value(request.getCurrency()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.toString()))
                .andExpect(jsonPath("$.providerReference").value(pspPaymentResponse.getProviderReference()))
                .andExpect(jsonPath("$.failureReason").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        assertSuccessCreatePayment(result);
    }

    @Test
    void create_shouldCreateNewPayment_whenPspDelayedSuccess() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("DELAYED_SUCCESS");

        PspPaymentResponse pspPaymentResponse = PspPaymentResponse.builder()
                .providerReference("PSP-REF-DELAYED-1")
                .status("DELAYED_SUCCESS")
                .build();

        when(pspClient.processPayment(
                PspPaymentRequest.builder()
                        .invoiceId(request.getInvoiceId())
                        .customerId(request.getCustomerId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .pspScenario(request.getPspScenario())
                        .build()
        )).thenReturn(pspPaymentResponse);

        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-DELAYED-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.invoiceId").value(request.getInvoiceId().toString()))
                .andExpect(jsonPath("$.customerId").value(request.getCustomerId()))
                .andExpect(jsonPath("$.amount").value(request.getAmount().doubleValue()))
                .andExpect(jsonPath("$.currency").value(request.getCurrency()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.toString()))
                .andExpect(jsonPath("$.providerReference").value(pspPaymentResponse.getProviderReference()))
                .andExpect(jsonPath("$.failureReason").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        assertSuccessCreatePayment(result);
    }

    @Test
    void create_shouldMarkPaymentFailed_whenPspTimeout() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("TIMEOUT");

        when(pspClient.processPayment(
                PspPaymentRequest.builder()
                        .invoiceId(request.getInvoiceId())
                        .customerId(request.getCustomerId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .pspScenario(request.getPspScenario())
                        .build()
        )).thenThrow(new ResourceAccessException("PSP timeout"));


        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-TIMEOUT-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        assertEquals(1, paymentRepository.count());

        PaymentEntity savedPayment = paymentRepository.findAll().get(0);
        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());
        assertEquals("PSP timeout", savedPayment.getFailureReason());

        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
        assertEquals(1, idempotencyRecordRepository.count());
    }

    @Test
    void create_shouldMarkPaymentFailed_whenPspInvalidResponse() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("INVALID");

        when(pspClient.processPayment(
                PspPaymentRequest.builder()
                        .invoiceId(request.getInvoiceId())
                        .customerId(request.getCustomerId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .pspScenario(request.getPspScenario())
                        .build()
        )).thenThrow(new RuntimeException("PSP rejected request"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-INVALID-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        assertEquals(1, paymentRepository.count());

        PaymentEntity savedPayment = paymentRepository.findAll().get(0);
        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());
        assertEquals("PSP rejected request", savedPayment.getFailureReason());

        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void create_shouldReturnConflict_whenSameKeyUsedWithDifferentRequestBody() throws Exception {
        CreatePaymentRequest request1 = validPaymentRequest("SUCCESS");
        CreatePaymentRequest request2 = validPaymentRequest("SUCCESS");
        request2.setAmount(new BigDecimal("999.00"));

        when(pspClient.processPayment(
                PspPaymentRequest.builder()
                        .invoiceId(request1.getInvoiceId())
                        .customerId(request1.getCustomerId())
                        .amount(request1.getAmount())
                        .currency(request1.getCurrency())
                        .pspScenario(request1.getPspScenario())
                        .build()
        ))
                .thenReturn(PspPaymentResponse.builder()
                        .providerReference("PSP-REF-001")
                        .status("SUCCESS")
                        .build());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "REUSED-KEY-1")
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "REUSED-KEY-1")
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());

        assertEquals(1, paymentRepository.count());
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());
    }

    @Test
    void create_shouldReturnConflict_whenExistingIdempotencyRecordIsInProgress() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        String requestHash = requestHashService.generateHash(request);

        idempotencyRecordRepository.save(IdempotencyRecordEntity.builder()
                .idempotencyKey("INPROGRESS-KEY-1")
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .lockedAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "INPROGRESS-KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void create_shouldReturnConflict_whenExistingIdempotencyRecordHasNullPaymentId() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        String requestHash = requestHashService.generateHash(request);
        String idempotencyKey = "NULL-PAYMENT-ID-KEY";

        idempotencyRecordRepository.save(IdempotencyRecordEntity.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.COMPLETED)
                .responseHttpStatus(201)
                .responseBody("{}")
                .paymentId(null)
                .lockedAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturnConflict_whenExistingIdempotencyRecordPointsToMissingPayment() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        String requestHash = requestHashService.generateHash(request);

        idempotencyRecordRepository.save(IdempotencyRecordEntity.builder()
                .idempotencyKey("MISSING-PAYMENT-KEY")
                .requestHash(requestHash)
                .status(IdempotencyStatus.COMPLETED)
                .responseHttpStatus(201)
                .responseBody("{}")
                .paymentId(UUID.randomUUID())
                .lockedAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "MISSING-PAYMENT-KEY")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_shouldReturnExistingPayment_whenExistingIdempotencyRecordIsCompleted() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");

        PspPaymentResponse pspPaymentResponse = PspPaymentResponse.builder()
                .providerReference("PSP-REF-EXISTING")
                .status("SUCCESS")
                .build();

        when(pspClient.processPayment(
                PspPaymentRequest.builder()
                        .invoiceId(request.getInvoiceId())
                        .customerId(request.getCustomerId())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .pspScenario(request.getPspScenario())
                        .build()
        )).thenReturn(pspPaymentResponse);


        MvcResult firstResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "EXISTING-KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse firstResponse =
                objectMapper.readValue(firstResult.getResponse().getContentAsString(), PaymentResponse.class);

        MvcResult secondResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "EXISTING-KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // only if your exception handler / service returns same response with same code
                .andReturn();

        PaymentResponse secondResponse =
                objectMapper.readValue(secondResult.getResponse().getContentAsString(), PaymentResponse.class);

        assertEquals(firstResponse.getPaymentId(), secondResponse.getPaymentId());
        assertEquals(1, paymentRepository.count());
        assertEquals(1, idempotencyRecordRepository.count());
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());
    }

    @Test
    void create_shouldCreateOnlyOnePayment_whenTwoConcurrentRequestsUseSameKeyAndSameBody() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");

        when(pspClient.processPayment(any(PspPaymentRequest.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(300);
                    return PspPaymentResponse.builder()
                            .providerReference("PSP-CONCURRENT-1")
                            .status("SUCCESS")
                            .build();
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> task = () -> {
            ready.countDown();
            start.await();

            return mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .header(IDEMPOTENCY_KEY_HEADER, "CONCURRENT-SAME-KEY")
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        Future<Integer> f1 = executor.submit(task);
        Future<Integer> f2 = executor.submit(task);

        ready.await();
        start.countDown();

        int status1 = f1.get();
        int status2 = f2.get();

        assertTrue(status1 == 201 || status1 == 409);
        assertTrue(status2 == 201 || status2 == 409);

        assertEquals(1, paymentRepository.count());
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());

        executor.shutdown();
    }

    @Test
    void create_shouldReturnConflictForOneRequest_whenTwoConcurrentRequestsUseSameKeyAndDifferentBody() throws Exception {
        CreatePaymentRequest request1 = validPaymentRequest("SUCCESS");
        CreatePaymentRequest request2 = validPaymentRequest("SUCCESS");
        request2.setAmount(new BigDecimal("999.00"));

        when(pspClient.processPayment(any(PspPaymentRequest.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(300);
                    return PspPaymentResponse.builder()
                            .providerReference("PSP-CONCURRENT-2")
                            .status("SUCCESS")
                            .build();
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> task1 = () -> {
            ready.countDown();
            start.await();
            return mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .header(IDEMPOTENCY_KEY_HEADER, "CONCURRENT-DIFF-KEY")
                            .content(objectMapper.writeValueAsString(request1)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        Callable<Integer> task2 = () -> {
            ready.countDown();
            start.await();
            return mockMvc.perform(post("/api/v1/payments")
                            .contentType(APPLICATION_JSON)
                            .header(IDEMPOTENCY_KEY_HEADER, "CONCURRENT-DIFF-KEY")
                            .content(objectMapper.writeValueAsString(request2)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        Future<Integer> f1 = executor.submit(task1);
        Future<Integer> f2 = executor.submit(task2);

        ready.await();
        start.countDown();

        int status1 = f1.get();
        int status2 = f2.get();

        assertTrue((status1 == 201 && status2 == 409) || (status1 == 409 && status2 == 201));
        assertEquals(1, paymentRepository.count());

        executor.shutdown();
    }

    @Test
    void create_shouldReturnBadRequest_whenIdempotencyKeyMissing() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
        assertEquals(0, idempotencyRecordRepository.count());
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void create_shouldReturnBadRequest_whenIdempotencyKeyBlank() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "   ")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
        assertEquals(0, idempotencyRecordRepository.count());
    }

    @Test
    void create_shouldReturnBadRequest_whenInvoiceIdMissing() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        request.setInvoiceId(null);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void create_shouldReturnBadRequest_whenCustomerIdMissing() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        request.setCustomerId(null);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void create_shouldReturnBadRequest_whenAmountMissing() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        request.setAmount(null);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void create_shouldReturnBadRequest_whenAmountNegative() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        request.setAmount(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void create_shouldReturnBadRequest_whenCurrencyMissing() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");
        request.setCurrency(null);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "KEY-1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void create_shouldReturnBadRequest_whenJsonMalformed() throws Exception {
        String malformedJson = """
                {
                  "invoiceId": "INV-001",
                  "customerId": "CST-001-A",
                  "amount": 500.00,
                  "currency": "USD"
                """;

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "SOME_KEY")
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        assertEquals(0, paymentRepository.count());
    }

    @Test
    void getById_shouldReturnPayment_whenPaymentExists() throws Exception {
        CreatePaymentRequest request = validPaymentRequest("SUCCESS");

        when(pspClient.processPayment(any(PspPaymentRequest.class)))
                .thenReturn(PspPaymentResponse.builder()
                        .providerReference("PSP-GET-1")
                        .status("SUCCESS")
                        .build());

        MvcResult postResult = mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "GET-BY-ID-KEY")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse created =
                objectMapper.readValue(postResult.getResponse().getContentAsString(), PaymentResponse.class);

        MvcResult result = mockMvc.perform(get("/api/v1/payments/{paymentId}", created.getPaymentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(created.getPaymentId().toString()))
                .andExpect(jsonPath("$.invoiceId").value(request.getInvoiceId().toString()))
                .andExpect(jsonPath("$.customerId").value(request.getCustomerId()))
                .andExpect(jsonPath("$.amount").value(request.getAmount().doubleValue()))
                .andExpect(jsonPath("$.currency").value(request.getCurrency()))
                .andExpect(jsonPath("$.status").value(PaymentStatus.SUCCESS.toString()))
                .andExpect(jsonPath("$.providerReference").isNotEmpty())
                .andExpect(jsonPath("$.failureReason").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        assertSuccessPayment(result);
    }

    @Test
    void getById_shouldReturnNotFound_whenPaymentDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{paymentId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAll_shouldReturnAllPayments() throws Exception {
        when(pspClient.processPayment(any(PspPaymentRequest.class)))
                .thenReturn(PspPaymentResponse.builder()
                        .providerReference("PSP-LIST")
                        .status("SUCCESS")
                        .build());

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "LIST-KEY-1")
                        .content(objectMapper.writeValueAsString(validPaymentRequest("SUCCESS"))))
                .andExpect(status().isCreated());

        CreatePaymentRequest request2 = validPaymentRequest("SUCCESS");
        request2.setInvoiceId("INV-002");

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY_HEADER, "LIST-KEY-2")
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private CreatePaymentRequest validPaymentRequest(String pspScenario) {
        return CreatePaymentRequest.builder()
                .invoiceId("INV-001")
                .customerId("CST-001-A")
                .amount(new BigDecimal("500.0"))
                .currency("USD")
                .pspScenario(pspScenario)
                .build();
    }

    private void assertSuccessCreatePayment(MvcResult result) throws UnsupportedEncodingException {
        assertSuccessPayment(result);
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());
    }

    private void assertSuccessPayment(MvcResult result) throws UnsupportedEncodingException {
        PaymentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse.class);

        Optional<PaymentEntity> optionalPayment = paymentRepository.findById(response.getPaymentId());
        assertTrue(optionalPayment.isPresent());

        assertTrue(optionalPayment.isPresent());

        PaymentEntity savedPayment = optionalPayment.get();
        assertEquals(response.getPaymentId(), savedPayment.getId());
        assertEquals(response.getInvoiceId(), savedPayment.getInvoiceId());
        assertEquals(response.getCustomerId(), savedPayment.getCustomerId());
        assertEquals(0, response.getAmount().compareTo(savedPayment.getAmount()));
        assertEquals(response.getCurrency(), savedPayment.getCurrency());
        assertEquals(response.getStatus(), savedPayment.getStatus().toString());
        assertEquals(response.getProviderReference(), savedPayment.getProviderReference());
        assertEquals(response.getFailureReason(), savedPayment.getFailureReason());

        assertEquals(1, paymentRepository.count());
        assertEquals(1, idempotencyRecordRepository.count());
    }
}
