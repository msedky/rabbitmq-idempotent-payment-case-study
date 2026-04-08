package com.rabbitmqidempotency.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmqidempotency.paymentservice.model.dto.request.CreatePaymentRequest;
import com.rabbitmqidempotency.paymentservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
public class RequestHashService {

    private final ObjectMapper objectMapper;

    public String generateHash(CreatePaymentRequest request) {
        try {
            String canonicalJson = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashedBytes);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize request for hashing");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}