package com.inventory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributesDto {

    private boolean perishable;
    private Integer shelfLifeDays;
    private boolean requiresPrescription;
    private boolean seasonal;
}
