package com.rabbitmqidempotency.pspmockservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqidempotency.pspmockservice.model.dto.request.PspPaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PspMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSuccessScenario() throws Exception {
        mockMvc.perform(post("/api/v1/psp/payments")
                        .header("X-PSP-Scenario", "SUCCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.providerReference").exists())
                .andExpect(jsonPath("$.processedAt").exists());
    }

    @Test
    void testUnknownScenarioDefaultsToSuccess() throws Exception {
        mockMvc.perform(post("/api/v1/psp/payments")
                        .header("X-PSP-Scenario", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    private PspPaymentRequest buildRequest() {
        return PspPaymentRequest.builder()
                .invoiceId("INV-123")
                .customerId("CUST-456")
                .amount(BigDecimal.valueOf(100))
                .currency("USD")
                .build();
    }
}
