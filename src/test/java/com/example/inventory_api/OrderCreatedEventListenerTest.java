package com.example.inventory_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

class OrderCreatedEventListenerTest {

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;
    private OrderCreatedEventListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        listener = new OrderCreatedEventListener(
                objectMapper,
                jdbcTemplate
        );
    }

    @Test
    void recordsNewEvent() throws Exception {
        OrderCreatedEvent event =
                new OrderCreatedEvent(1001, 404, 5001, 2, "CREATED");

        when(objectMapper.readValue(
                anyString(),
                eq(OrderCreatedEvent.class)
        )).thenReturn(event);

        when(jdbcTemplate.update(
                anyString(),
                any(Object[].class)
        )).thenReturn(1);

        String output = captureOutput(
                () -> listener.handleOrderCreated("{}")
        );

        verify(jdbcTemplate).update(
                contains("INSERT INTO processed_order_events"),
                eq((Object) 1001)
        );

        assertTrue(output.contains("Order event received!"));
        assertTrue(output.contains("Order ID: 1001"));
    }

    @Test
    void ignoresDuplicateEvent() throws Exception {
        OrderCreatedEvent event =
                new OrderCreatedEvent(1001, 404, 5001, 2, "CREATED");

        when(objectMapper.readValue(
                anyString(),
                eq(OrderCreatedEvent.class)
        )).thenReturn(event);

        when(jdbcTemplate.update(
                anyString(),
                any(Object[].class)
        )).thenReturn(0);

        String output = captureOutput(
                () -> listener.handleOrderCreated("{}")
        );

        assertTrue(
                output.contains(
                        "Duplicate event ignored for order ID: 1001"
                )
        );

        assertFalse(output.contains("Order event received!"));
    }

    @Test
    void propagatesDatabaseFailure() throws Exception {
        OrderCreatedEvent event =
                new OrderCreatedEvent(1002, 404, 5001, 2, "CREATED");

        when(objectMapper.readValue(
                anyString(),
                eq(OrderCreatedEvent.class)
        )).thenReturn(event);

        when(jdbcTemplate.update(
                anyString(),
                any(Object[].class)
        )).thenThrow(
                new DataAccessResourceFailureException(
                        "Simulated database connection failure"
                )
        );

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> listener.handleOrderCreated("{}")
        );
    }

    private String captureOutput(ThrowingAction action) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (PrintStream capture =
                     new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}