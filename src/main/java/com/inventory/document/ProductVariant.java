package com.inventory.document;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    private String size;
    private String unit;
    private BigDecimal mrp;
}
