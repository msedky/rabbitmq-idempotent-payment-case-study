package com.rabbitmqidempotency.paymentservice.exception.handler;

import com.rabbitmqidempotency.paymentservice.exception.BadRequestException;
import com.rabbitmqidempotency.paymentservice.exception.ConflictException;
import com.rabbitmqidempotency.paymentservice.model.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        return ErrorResponse.builder()
                .code("BAD_REQUEST")
                .message(ex.getMessage())
                .timestamp(OffsetDateTime.now())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(ConflictException ex) {
        return ErrorResponse.builder()
                .code("CONFLICT")
                .message(ex.getMessage())
                .timestamp(OffsetDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed");

        return ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message(ex.getMessage())
                .timestamp(OffsetDateTime.now())
                .build();
    }
}