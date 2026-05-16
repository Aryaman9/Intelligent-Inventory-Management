package com.inventory.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UpdateProductRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String category;

    @Size(max = 100)
    private String brand;

    private String barcode;

    private List<ProductVariantDto> variants;

    private ProductAttributesDto attributes;

    private List<String> tags;
}
