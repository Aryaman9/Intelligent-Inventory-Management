package com.inventory.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatsResponse {

    private long totalItems;
    private BigDecimal totalValue;
    private long lowStockCount;
    private long expiringCount;
}
