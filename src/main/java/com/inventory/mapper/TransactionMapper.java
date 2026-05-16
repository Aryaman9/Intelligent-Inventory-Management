package com.inventory.mapper;

import com.inventory.dto.response.TransactionResponse;
import com.inventory.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "storeName", source = "store.name")
    @Mapping(target = "inventoryId", source = "inventory.id")
    @Mapping(target = "productName", ignore = true)
    TransactionResponse toResponse(Transaction transaction);
}
