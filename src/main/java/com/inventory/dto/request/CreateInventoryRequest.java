package com.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryRequest {

    @NotNull
    private UUID storeId;

    @NotBlank
    private String productId;

    @NotNull
    @DecimalMin("0")
    private BigDecimal quantity;

    @Size(max = 50)
    private String unit;

    @DecimalMin("0")
    private BigDecimal lowStockThreshold;

    private BigDecimal reorderQuantity;

    @NotNull
    @DecimalMin("0")
    private BigDecimal costPrice;

    @NotNull
    @DecimalMin("0")
    private BigDecimal sellingPrice;

    private BigDecimal mrp;
    private String batchNumber;
    private LocalDate expiryDate;
    private String location;
}
