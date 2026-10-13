package com.example.inventory_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import tools.jackson.databind.ObjectMapper;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;

@Testcontainers
class ProcessedOrderEventIntegrationTest {

    private JdbcTemplate jdbcTemplate;

    private OrderCreatedEventListener listener;

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource();

        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        this.jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute(
                "DROP TABLE IF EXISTS processed_order_events"
        );

        jdbcTemplate.execute("""
                CREATE TABLE processed_order_events (
                    order_id INTEGER PRIMARY KEY,
                    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        this.listener = new OrderCreatedEventListener(
                new ObjectMapper(),
                jdbcTemplate
        );
    }

    @Test
    void duplicateOrderEventIsProcessedOnlyOnce() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent(9002, 404, 5001, 2, "CREATED");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);

            String message = new ObjectMapper().writeValueAsString(event);

            listener.handleOrderCreated(message);
            listener.handleOrderCreated(message);

        } finally {
            System.setOut(originalOut);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_order_events WHERE order_id = ?",
                Integer.class,
                9002
        );

        assertEquals(1, count);

        String consoleOutput = output.toString(StandardCharsets.UTF_8);

        assertEquals(
                1,
                consoleOutput.split("Order event received!", -1).length - 1
        );

        assertTrue(
                consoleOutput.contains(
                        "Duplicate event ignored for order ID: 9002"
                )
        );
    }
}