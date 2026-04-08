package com.rabbitmqidempotency.paymentservice.repository;

import com.rabbitmqidempotency.paymentservice.model.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {
    Optional<IdempotencyRecordEntity> findByIdempotencyKey(String idempotencyKey);
}