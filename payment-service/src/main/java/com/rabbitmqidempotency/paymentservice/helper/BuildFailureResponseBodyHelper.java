package com.rabbitmqidempotency.paymentservice.helper;

import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class BuildFailureResponseBodyHelper implements Function<String, String> {
    @Override
    public String apply(String message) {
        return """
                {
                  "code": "PAYMENT_PROCESSING_FAILED",
                  "message": "%s"
                }
                """.formatted(escapeJson(message));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\\\"");
    }
}
