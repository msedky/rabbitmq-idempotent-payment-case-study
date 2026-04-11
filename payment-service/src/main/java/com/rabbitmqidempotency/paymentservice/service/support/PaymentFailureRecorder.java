package com.rabbitmqidempotency.paymentservice.service.support;

import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import com.rabbitmqidempotency.paymentservice.model.enums.IdempotencyStatus;
import com.rabbitmqidempotency.paymentservice.model.enums.PaymentStatus;
import com.rabbitmqidempotency.paymentservice.repository.IdempotencyRecordRepository;
import com.rabbitmqidempotency.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final Function<String, String> buildFailureResponseBodyHelper;

    @Transactional/*(propagation = Propagation.REQUIRES_NEW)*/
    public void recordFailure(
            PaymentEntity payment,
            IdempotencyRecordEntity idempotencyRecord,
            String failureReason,
            int httpStatus
    ) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);

        idempotencyRecord.setStatus(IdempotencyStatus.FAILED);
        idempotencyRecord.setResponseHttpStatus(httpStatus);
        idempotencyRecord.setResponseBody(buildFailureResponseBodyHelper.apply(failureReason));
        idempotencyRecord.setPaymentId(payment.getId());
        idempotencyRecordRepository.save(idempotencyRecord);
    }
}