package com.inventory.dto.response;

import com.inventory.dto.request.ProductAttributesDto;
import com.inventory.dto.request.ProductVariantDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String name;
    private String category;
    private String brand;
    private String barcode;
    private List<ProductVariantDto> variants;
    private ProductAttributesDto attributes;
    private List<String> tags;
    private boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
