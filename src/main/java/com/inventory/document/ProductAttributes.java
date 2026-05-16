package com.inventory.document;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributes {

    private boolean perishable;
    private Integer shelfLifeDays;
    private boolean requiresPrescription;
    private boolean seasonal;
}
