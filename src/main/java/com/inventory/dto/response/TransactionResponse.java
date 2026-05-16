package com.inventory.dto.response;

import com.inventory.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private UUID storeId;
    private String storeName;
    private UUID inventoryId;
    private String productName;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String customerName;
    private String customerPhone;
    private String invoiceNumber;
    private String idempotencyKey;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
