package com.inventory.mapper;

import com.inventory.dto.response.InventoryResponse;
import com.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    // source "active" = Inventory getter isActive() → MapStruct property "active"
    // target "isActive" = InventoryResponse builder method isActive() → property "isActive"
    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "storeName", source = "store.name")
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "productCategory", ignore = true)
    @Mapping(target = "productBrand", ignore = true)
    @Mapping(target = "lowStock", ignore = true)
    InventoryResponse toResponse(Inventory inventory);
}
