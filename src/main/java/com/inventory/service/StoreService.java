package com.inventory.service;

import com.inventory.dto.request.CreateStoreRequest;
import com.inventory.dto.request.UpdateStoreRequest;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.dto.response.StoreResponse;
import com.inventory.dto.response.StoreStatsResponse;
import com.inventory.entity.Store;
import com.inventory.entity.User;
import com.inventory.event.UserActionAuditEvent;
import com.inventory.exception.ForbiddenException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.StoreMapper;
import com.inventory.repository.jpa.StoreRepository;
import com.inventory.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreMapper storeMapper;
    private final ApplicationEventPublisher eventPublisher;

    public StoreResponse createStore(CreateStoreRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Store store = storeMapper.toEntity(request);
        store.setUser(user);
        Store saved = storeRepository.save(store);

        eventPublisher.publishEvent(UserActionAuditEvent.builder()
                .userId(userId)
                .action("STORE_CREATED")
                .resourceType("Store")
                .resourceId(saved.getId().toString())
                .metadata(Map.of("name", saved.getName()))
                .build());

        return storeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreById(UUID storeId, UUID userId) {
        Store store = findStore(storeId);
        verifyOwnership(store, userId);
        return storeMapper.toResponse(store);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<StoreResponse> getUserStores(UUID userId, String search, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Store> storePage;
        if (search != null && !search.isBlank()) {
            storePage = storeRepository.findByUserIdAndIsActiveTrueAndNameContainingIgnoreCase(userId, search, pageable);
        } else {
            storePage = storeRepository.findByUserIdAndIsActiveTrue(userId, pageable);
        }
        Page<StoreResponse> responsePage = storePage.map(storeMapper::toResponse);
        return PaginatedResponse.<StoreResponse>builder()
                .items(responsePage.getContent())
                .pagination(PaginatedResponse.PaginationInfo.builder()
                        .total(responsePage.getTotalElements())
                        .page(page)
                        .limit(limit)
                        .pages(responsePage.getTotalPages())
                        .build())
                .build();
    }

    public StoreResponse updateStore(UUID storeId, UpdateStoreRequest request, UUID userId) {
        Store store = findStore(storeId);
        verifyOwnership(store, userId);
        storeMapper.updateEntity(request, store);
        Store saved = storeRepository.save(store);

        eventPublisher.publishEvent(UserActionAuditEvent.builder()
                .userId(userId)
                .action("STORE_UPDATED")
                .resourceType("Store")
                .resourceId(storeId.toString())
                .metadata(Map.of("name", saved.getName()))
                .build());

        return storeMapper.toResponse(saved);
    }

    public void deleteStore(UUID storeId, UUID userId) {
        Store store = findStore(storeId);
        verifyOwnership(store, userId);
        store.setActive(false);
        storeRepository.save(store);

        eventPublisher.publishEvent(UserActionAuditEvent.builder()
                .userId(userId)
                .action("STORE_DELETED")
                .resourceType("Store")
                .resourceId(storeId.toString())
                .metadata(Map.of())
                .build());
    }

    @Transactional(readOnly = true)
    public StoreStatsResponse getStoreStats(UUID userId) {
        List<Store> allStores = storeRepository.findByUserId(userId);
        long totalActive = allStores.stream().filter(Store::isActive).count();
        long totalInactive = allStores.size() - totalActive;
        Map<String, Long> countsByType = allStores.stream()
                .filter(Store::isActive)
                .collect(Collectors.groupingBy(Store::getType, Collectors.counting()));
        return StoreStatsResponse.builder()
                .countsByType(countsByType)
                .totalActive(totalActive)
                .totalInactive(totalInactive)
                .build();
    }

    private Store findStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
    }

    private void verifyOwnership(Store store, UUID userId) {
        if (!store.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this store");
        }
    }
}
