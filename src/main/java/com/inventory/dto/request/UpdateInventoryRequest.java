package com.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryRequest {

    @DecimalMin("0")
    private BigDecimal quantity;

    @Size(max = 50)
    private String unit;

    @DecimalMin("0")
    private BigDecimal lowStockThreshold;

    private BigDecimal reorderQuantity;

    @DecimalMin("0")
    private BigDecimal costPrice;

    @DecimalMin("0")
    private BigDecimal sellingPrice;

    private BigDecimal mrp;
    private String batchNumber;
    private LocalDate expiryDate;
    private String location;
}
