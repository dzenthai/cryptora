package com.dzenthai.cryptora.message;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class AmqpDispatcher {

    private final AmqpTemplate amqpTemplate;

    @Value("${spring.rabbitmq.queue.name}")
    private String queueName;

    public AmqpDispatcher(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void send(String message) {
        amqpTemplate.convertAndSend(queueName, message);
    }
}
