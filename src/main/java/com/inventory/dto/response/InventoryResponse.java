package com.inventory.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID id;
    private UUID storeId;
    private String storeName;
    private String productId;
    private String productName;
    private String productCategory;
    private String productBrand;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal lowStockThreshold;
    private BigDecimal reorderQuantity;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private String batchNumber;
    private LocalDate expiryDate;
    private LocalDateTime lastRestockedAt;
    private LocalDateTime lastSoldAt;
    private String location;
    private boolean isActive;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean lowStock;
}
