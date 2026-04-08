package com.rabbitmqidempotency.paymentservice.mapper;

import com.rabbitmqidempotency.paymentservice.model.dto.response.PaymentResponse;
import com.rabbitmqidempotency.paymentservice.model.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "status", expression = "java(paymentEntity.getStatus().name())")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.ofInstant(paymentEntity.getCreatedAt(), java.time.ZoneId.systemDefault()))")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.ofInstant(paymentEntity.getUpdatedAt(), java.time.ZoneId.systemDefault()))")
    PaymentResponse toResponse(PaymentEntity paymentEntity);

    List<PaymentResponse> toResponseList(List<PaymentEntity> paymentEntities);
}