package com.inventory.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @NotNull
    private UUID originalTransactionId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    private String reason;
    private String notes;
}
