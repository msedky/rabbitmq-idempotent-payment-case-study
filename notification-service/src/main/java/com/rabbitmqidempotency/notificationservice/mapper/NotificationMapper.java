package com.rabbitmqidempotency.notificationservice.mapper;

import com.rabbitmqidempotency.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqidempotency.notificationservice.model.entity.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponseDto toResponseDto(NotificationEntity entity);
}