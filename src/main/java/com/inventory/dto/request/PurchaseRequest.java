package com.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {

    @NotNull
    private UUID inventoryId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal pricePerUnit;

    private String notes;
}
