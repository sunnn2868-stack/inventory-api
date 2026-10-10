package com.example.inventory_api;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;


@Component
public class OrderCreatedEventListener {

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public OrderCreatedEventListener( ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @RabbitListener(queues = "order.created")
    @Transactional public void handleOrderCreated(String message) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

        int inserted = jdbcTemplate.update(
                "INSERT INTO processed_order_events (order_id, processed_at) " +
                        "VALUES (?, CURRENT_TIMESTAMP) " +
                        "ON CONFLICT (order_id) DO NOTHING", event.orderId());

        if (inserted == 0) {
            System.out.println( "Duplicate event ignored for order ID: "
                    + event.orderId()
            );
            return;
        }

        System.out.println("Order event received!");
        System.out.println("Order ID: " + event.orderId());
        System.out.println("Customer ID: " + event.customerId());
        System.out.println("Product ID: " + event.productId());
        System.out.println("Quantity: " + event.quantity());
        System.out.println("Status: " + event.status());
    }
}