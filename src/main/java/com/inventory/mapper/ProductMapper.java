package com.inventory.mapper;

import com.inventory.document.Product;
import com.inventory.document.ProductAttributes;
import com.inventory.document.ProductVariant;
import com.inventory.dto.request.CreateProductRequest;
import com.inventory.dto.request.ProductAttributesDto;
import com.inventory.dto.request.ProductVariantDto;
import com.inventory.dto.request.UpdateProductRequest;
import com.inventory.dto.response.ProductResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toDocument(CreateProductRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateDocument(UpdateProductRequest request, @MappingTarget Product product);

    ProductVariantDto toVariantDto(ProductVariant variant);

    ProductVariant toVariant(ProductVariantDto dto);

    ProductAttributesDto toAttributesDto(ProductAttributes attributes);

    ProductAttributes toAttributes(ProductAttributesDto dto);
}
