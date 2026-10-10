package com.example.inventory_api.controller;

import com.example.inventory_api.model.Inventory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.bind.annotation.RequestHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class InventoryController {

    private boolean simulateFailure = false;

    private static final Logger logger =
            LoggerFactory.getLogger(InventoryController.class);

    @GetMapping("/inventory/{productId}")
    public Inventory getInventory(
            @PathVariable int productId,
            @RequestHeader(value = "X-Correlation-ID", required = false)
            String correlationId) {

        logger.info(
                "Received inventory request: productId={}, correlationId={}",
                productId,
                correlationId
        );

        if (simulateFailure) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Inventory service temporarily unavailable"
            );
        }

        Inventory inventory = new Inventory();

        inventory.setProductId(productId);

        if (productId == 5001) {
            inventory.setAvailable(true);
            inventory.setQuantity(10);
        } else {
            inventory.setAvailable(false);
            inventory.setQuantity(0);
        }

        return inventory;
    }

    @PostMapping("/inventory/failure")
    public String toggleFailure() {
        simulateFailure = !simulateFailure;
        return "simulateFailure = " + simulateFailure;
    }
}