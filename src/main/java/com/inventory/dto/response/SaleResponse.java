package com.inventory.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {

    private TransactionResponse transaction;
    private BigDecimal inventoryQuantity;
    private String productName;
}
