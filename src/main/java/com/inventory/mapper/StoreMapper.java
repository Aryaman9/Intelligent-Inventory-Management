package com.inventory.mapper;

import com.inventory.dto.request.CreateStoreRequest;
import com.inventory.dto.request.UpdateStoreRequest;
import com.inventory.dto.response.StoreResponse;
import com.inventory.entity.Store;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    @Mapping(target = "ownerId", source = "user.id")
    @Mapping(target = "ownerName", source = "user.fullName")
    @Mapping(target = "isActive", source = "active")
    StoreResponse toResponse(Store store);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "version", ignore = true)
    Store toEntity(CreateStoreRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(UpdateStoreRequest request, @MappingTarget Store store);
}
