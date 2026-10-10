package com.example.inventory_api;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_order_events")
public class ProcessedOrderEvent {

    @Id
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    protected ProcessedOrderEvent() {
    }

    public ProcessedOrderEvent(Integer orderId) {
        this.orderId = orderId;
        this.processedAt = LocalDateTime.now();
    }

    public Integer getOrderId() {
        return orderId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}