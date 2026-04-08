package com.rabbitmqidempotency.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(RabbitMqConstants.PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return new Queue(RabbitMqConstants.PAYMENT_COMPLETED_QUEUE);
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentCompletedQueue)
                .to(paymentExchange)
                .with(RabbitMqConstants.PAYMENT_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}