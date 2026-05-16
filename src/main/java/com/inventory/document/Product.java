package com.inventory.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String category;

    private String brand;

    @Indexed(unique = true, sparse = true)
    private String barcode;

    private List<ProductVariant> variants;

    private ProductAttributes attributes;

    @Indexed
    private List<String> tags;

    @Builder.Default
    private boolean active = true;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
