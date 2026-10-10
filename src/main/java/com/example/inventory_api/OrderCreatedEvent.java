package com.example.inventory_api;

public record OrderCreatedEvent(
        Integer orderId,
        Integer customerId,
        Integer productId,
        Integer quantity,
        String status
) {
}