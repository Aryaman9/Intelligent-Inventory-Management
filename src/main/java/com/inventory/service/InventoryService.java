package com.inventory.service;

import com.inventory.cache.CacheService;
import com.inventory.document.Product;
import com.inventory.dto.request.CreateInventoryRequest;
import com.inventory.dto.request.UpdateInventoryRequest;
import com.inventory.dto.response.AlertsResponse;
import com.inventory.dto.response.InventoryResponse;
import com.inventory.dto.response.InventoryStatsResponse;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.entity.Inventory;
import com.inventory.entity.Store;
import com.inventory.exception.ConflictException;
import com.inventory.exception.ErrorCode;
import com.inventory.exception.ForbiddenException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.InventoryMapper;
import com.inventory.repository.jpa.InventoryRepository;
import com.inventory.repository.jpa.StoreRepository;
import com.inventory.resilience.MongoProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepo;
    private final StoreRepository storeRepo;
    private final MongoProductClient mongoProductClient;
    private final InventoryMapper inventoryMapper;
    private final CacheService cacheService;

    public InventoryResponse createInventory(CreateInventoryRequest request, UUID userId) {
        Store store = storeRepo.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store", request.getStoreId()));

        if (!store.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this store");
        }

        mongoProductClient.getById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        inventoryRepo.findByStoreIdAndProductId(request.getStoreId(), request.getProductId())
                .ifPresent(existing -> {
                    throw new ConflictException("Inventory already exists for this product in the store",
                            ErrorCode.INV_002);
                });

        Inventory inv = Inventory.builder()
                .store(store)
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .unit(request.getUnit() != null ? request.getUnit() : "piece")
                .lowStockThreshold(request.getLowStockThreshold() != null
                        ? request.getLowStockThreshold() : BigDecimal.TEN)
                .reorderQuantity(request.getReorderQuantity())
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .mrp(request.getMrp())
                .batchNumber(request.getBatchNumber())
                .expiryDate(request.getExpiryDate())
                .location(request.getLocation())
                .build();

        inv = inventoryRepo.save(inv);
        return enrich(inv, mongoProductClient.getById(request.getProductId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(UUID inventoryId, UUID userId) {
        Inventory inv = findWithOwnership(inventoryId, userId);
        Product product = mongoProductClient.getById(inv.getProductId()).orElse(null);
        return enrich(inv, product);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<InventoryResponse> getStoreInventory(
            UUID storeId, UUID userId, boolean lowStockOnly, int page, int limit) {

        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        if (!store.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this store");
        }

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Inventory> inventoryPage = lowStockOnly
                ? inventoryRepo.findLowStockByStoreId(storeId, pageable)
                : inventoryRepo.findByStoreIdAndIsActiveTrue(storeId, pageable);

        // Batch-fetch products from MongoDB
        List<String> productIds = inventoryPage.getContent().stream()
                .map(Inventory::getProductId)
                .collect(Collectors.toList());
        Map<String, Product> productMap = new HashMap<>();
        mongoProductClient.getByIds(productIds)
                .forEach(p -> productMap.put(p.getId(), p));

        List<InventoryResponse> items = inventoryPage.getContent().stream()
                .map(inv -> enrich(inv, productMap.get(inv.getProductId())))
                .collect(Collectors.toList());

        return PaginatedResponse.<InventoryResponse>builder()
                .items(items)
                .pagination(PaginatedResponse.PaginationInfo.builder()
                        .total(inventoryPage.getTotalElements())
                        .page(page)
                        .limit(limit)
                        .pages(inventoryPage.getTotalPages())
                        .build())
                .build();
    }

    public InventoryResponse updateInventory(UUID inventoryId, UpdateInventoryRequest request, UUID userId) {
        Inventory inv = findWithOwnership(inventoryId, userId);

        if (request.getQuantity() != null) inv.setQuantity(request.getQuantity());
        if (request.getUnit() != null) inv.setUnit(request.getUnit());
        if (request.getLowStockThreshold() != null) inv.setLowStockThreshold(request.getLowStockThreshold());
        if (request.getReorderQuantity() != null) inv.setReorderQuantity(request.getReorderQuantity());
        if (request.getCostPrice() != null) inv.setCostPrice(request.getCostPrice());
        if (request.getSellingPrice() != null) inv.setSellingPrice(request.getSellingPrice());
        if (request.getMrp() != null) inv.setMrp(request.getMrp());
        if (request.getBatchNumber() != null) inv.setBatchNumber(request.getBatchNumber());
        if (request.getExpiryDate() != null) inv.setExpiryDate(request.getExpiryDate());
        if (request.getLocation() != null) inv.setLocation(request.getLocation());

        inv = inventoryRepo.save(inv);
        Product product = mongoProductClient.getById(inv.getProductId()).orElse(null);
        return enrich(inv, product);
    }

    public void deleteInventory(UUID inventoryId, UUID userId) {
        Inventory inv = findWithOwnership(inventoryId, userId);
        inv.setActive(false);
        inventoryRepo.save(inv);
    }

    @Transactional(readOnly = true)
    public InventoryStatsResponse getInventoryStats(UUID storeId, UUID userId) {
        Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        if (!store.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this store");
        }

        String cacheKey = "stats:store:" + storeId;
        return cacheService.getOrLoad(cacheKey, Duration.ofMinutes(15),
                InventoryStatsResponse.class, () -> computeStats(storeId));
    }

    @Transactional(readOnly = true)
    public AlertsResponse getAlerts(UUID userId) {
        List<Store> userStores = storeRepo.findByUserId(userId);
        if (userStores.isEmpty()) {
            return AlertsResponse.builder()
                    .lowStockAlerts(List.of())
                    .expiryAlerts(List.of())
                    .summary(AlertsResponse.Summary.builder().lowStockCount(0).expiringSoonCount(0).build())
                    .build();
        }

        List<UUID> storeIds = userStores.stream().map(Store::getId).collect(Collectors.toList());
        Map<UUID, Store> storeMap = userStores.stream().collect(Collectors.toMap(Store::getId, s -> s));

        // Authoritative check from Postgres
        List<Inventory> lowStockItems = inventoryRepo.findLowStockByStoreIds(storeIds);
        LocalDate today = LocalDate.now();
        List<Inventory> expiringItems = inventoryRepo.findExpiringByStoreIds(storeIds, today.plusDays(7), today);

        // Batch-fetch product names from MongoDB
        Set<String> productIdSet = new HashSet<>();
        lowStockItems.forEach(i -> productIdSet.add(i.getProductId()));
        expiringItems.forEach(i -> productIdSet.add(i.getProductId()));

        Map<String, Product> productMap = new HashMap<>();
        if (!productIdSet.isEmpty()) {
            mongoProductClient.getByIds(new ArrayList<>(productIdSet))
                    .forEach(p -> productMap.put(p.getId(), p));
        }

        List<AlertsResponse.LowStockAlert> lowStockAlerts = lowStockItems.stream()
                .map(inv -> {
                    Product product = productMap.get(inv.getProductId());
                    Store store = storeMap.get(inv.getStore().getId());
                    BigDecimal shortage = inv.getLowStockThreshold().subtract(inv.getQuantity());
                    return AlertsResponse.LowStockAlert.builder()
                            .inventoryId(inv.getId())
                            .quantity(inv.getQuantity())
                            .lowStockThreshold(inv.getLowStockThreshold())
                            .shortage(shortage.max(BigDecimal.ZERO))
                            .product(AlertsResponse.ProductInfo.builder()
                                    .id(inv.getProductId())
                                    .name(product != null ? product.getName() : "Unknown")
                                    .build())
                            .store(AlertsResponse.StoreInfo.builder()
                                    .id(store != null ? store.getId() : inv.getStore().getId())
                                    .name(store != null ? store.getName() : "Unknown")
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());

        List<AlertsResponse.ExpiryAlert> expiryAlerts = expiringItems.stream()
                .map(inv -> {
                    Product product = productMap.get(inv.getProductId());
                    Store store = storeMap.get(inv.getStore().getId());
                    long daysUntilExpiry = ChronoUnit.DAYS.between(today, inv.getExpiryDate());
                    return AlertsResponse.ExpiryAlert.builder()
                            .inventoryId(inv.getId())
                            .expiryDate(inv.getExpiryDate())
                            .daysUntilExpiry(daysUntilExpiry)
                            .product(AlertsResponse.ProductInfo.builder()
                                    .id(inv.getProductId())
                                    .name(product != null ? product.getName() : "Unknown")
                                    .build())
                            .store(AlertsResponse.StoreInfo.builder()
                                    .id(store != null ? store.getId() : inv.getStore().getId())
                                    .name(store != null ? store.getName() : "Unknown")
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());

        return AlertsResponse.builder()
                .lowStockAlerts(lowStockAlerts)
                .expiryAlerts(expiryAlerts)
                .summary(AlertsResponse.Summary.builder()
                        .lowStockCount(lowStockAlerts.size())
                        .expiringSoonCount(expiryAlerts.size())
                        .build())
                .build();
    }

    private InventoryStatsResponse computeStats(UUID storeId) {
        long totalItems = inventoryRepo.countByStoreIdAndIsActiveTrue(storeId);
        BigDecimal totalValue = inventoryRepo.getTotalValueByStoreId(storeId);
        long lowStockCount = inventoryRepo.countLowStockByStoreId(storeId);
        long expiringCount = inventoryRepo.countExpiringByStoreId(storeId, LocalDate.now().plusDays(7));

        return InventoryStatsResponse.builder()
                .totalItems(totalItems)
                .totalValue(totalValue != null ? totalValue : BigDecimal.ZERO)
                .lowStockCount(lowStockCount)
                .expiringCount(expiringCount)
                .build();
    }

    private Inventory findWithOwnership(UUID inventoryId, UUID userId) {
        Inventory inv = inventoryRepo.findByIdWithStoreAndUser(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        if (!inv.getStore().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this inventory");
        }
        return inv;
    }

    private InventoryResponse enrich(Inventory inv, Product product) {
        InventoryResponse response = inventoryMapper.toResponse(inv);
        if (product != null) {
            response.setProductName(product.getName());
            response.setProductCategory(product.getCategory());
            response.setProductBrand(product.getBrand());
        }
        response.setLowStock(inv.getQuantity().compareTo(inv.getLowStockThreshold()) <= 0);
        return response;
    }
}
