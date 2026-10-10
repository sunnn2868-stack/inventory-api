package com.example.inventory_api;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedOrderEventRepository
        extends JpaRepository<ProcessedOrderEvent, Integer> {
}