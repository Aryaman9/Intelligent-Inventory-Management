package com.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class InventoryUpdatedEvent {
    private final UUID inventoryId;
    private final UUID storeId;
    private final BigDecimal newQuantity;
    private final BigDecimal lowStockThreshold;
    private final LocalDate expiryDate;
    private final String productId;
    private final String action; // "SALE", "PURCHASE", "RETURN", "ADJUSTMENT"
}
