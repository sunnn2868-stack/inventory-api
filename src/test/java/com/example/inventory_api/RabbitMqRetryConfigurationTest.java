package com.example.inventory_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("local")
class RabbitMqRetryConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void rabbitMqRetryPropertiesAreConfigured() {

        assertEquals(
                "local",
                environment.getActiveProfiles()[0]
        );

        assertEquals(
                "true",
                environment.getProperty(
                        "spring.rabbitmq.listener.simple.retry.enabled"
                )
        );

        assertEquals(
                "1s",
                environment.getProperty(
                        "spring.rabbitmq.listener.simple.retry.initial-interval"
                )
        );

        assertEquals(
                "3",
                environment.getProperty(
                        "spring.rabbitmq.listener.simple.retry.max-attempts"
                )
        );

        assertEquals(
                "2",
                environment.getProperty(
                        "spring.rabbitmq.listener.simple.retry.multiplier"
                )
        );

        assertEquals(
                "false",
                environment.getProperty(
                        "spring.rabbitmq.listener.simple.default-requeue-rejected"
                )
        );

    }
}