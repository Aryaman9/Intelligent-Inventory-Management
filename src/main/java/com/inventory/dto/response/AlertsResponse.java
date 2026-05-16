package com.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertsResponse {

    private List<LowStockAlert> lowStockAlerts;
    private List<ExpiryAlert> expiryAlerts;
    private Summary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockAlert {
        private UUID inventoryId;
        private BigDecimal quantity;
        private BigDecimal lowStockThreshold;
        private BigDecimal shortage;
        private ProductInfo product;
        private StoreInfo store;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpiryAlert {
        private UUID inventoryId;
        private LocalDate expiryDate;
        private long daysUntilExpiry;
        private ProductInfo product;
        private StoreInfo store;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private int lowStockCount;
        private int expiringSoonCount;
    }
}
