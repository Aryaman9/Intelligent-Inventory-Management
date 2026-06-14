package com.inventory.service;

import com.inventory.cache.CacheService;
import com.inventory.document.Product;
import com.inventory.dto.request.CreateInventoryRequest;
import com.inventory.dto.response.InventoryResponse;
import com.inventory.dto.response.InventoryStatsResponse;
import com.inventory.entity.Inventory;
import com.inventory.entity.Store;
import com.inventory.entity.User;
import com.inventory.exception.ForbiddenException;
import com.inventory.mapper.InventoryMapper;
import com.inventory.repository.jpa.InventoryRepository;
import com.inventory.repository.jpa.StoreRepository;
import com.inventory.resilience.MongoProductClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepo;
    @Mock private StoreRepository storeRepo;
    @Mock private MongoProductClient mongoProductClient;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private CacheService cacheService;

    @InjectMocks private InventoryService inventoryService;

    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    private Store store(UUID ownerId) {
        User owner = User.builder().email("o@test.com").passwordHash("h").fullName("Owner").build();
        owner.setId(ownerId);
        Store s = Store.builder()
                .user(owner).name("S").type("KIRANA").address("a")
                .city("c").state("st").pincode("400001").build();
        s.setId(storeId);
        return s;
    }

    private CreateInventoryRequest createRequest() {
        return CreateInventoryRequest.builder()
                .storeId(storeId)
                .productId("p1")
                .quantity(new BigDecimal("50"))
                .costPrice(new BigDecimal("15"))
                .sellingPrice(new BigDecimal("20"))
                .lowStockThreshold(new BigDecimal("10"))
                .unit("piece")
                .build();
    }

    @Test
    void createInventory_happyPath_enrichesWithProductData() {
        when(storeRepo.findById(storeId)).thenReturn(Optional.of(store(userId)));
        when(mongoProductClient.getById("p1"))
                .thenReturn(Optional.of(Product.builder().id("p1").name("Widget")
                        .category("Grocery").brand("Acme").build()));
        when(inventoryRepo.findByStoreIdAndProductId(storeId, "p1")).thenReturn(Optional.empty());
        when(inventoryRepo.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryMapper.toResponse(any(Inventory.class)))
                .thenReturn(InventoryResponse.builder().build());

        InventoryResponse response = inventoryService.createInventory(createRequest(), userId);

        assertThat(response.getProductName()).isEqualTo("Widget");
        assertThat(response.getProductCategory()).isEqualTo("Grocery");
        assertThat(response.isLowStock()).isFalse(); // 50 > threshold 10
        verify(inventoryRepo).save(any(Inventory.class));
    }

    @Test
    void createInventory_notOwner_throwsForbidden() {
        when(storeRepo.findById(storeId)).thenReturn(Optional.of(store(UUID.randomUUID())));

        assertThatThrownBy(() -> inventoryService.createInventory(createRequest(), userId))
                .isInstanceOf(ForbiddenException.class);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    void getInventoryStats_computesAggregatesFromRepository() {
        when(storeRepo.findById(storeId)).thenReturn(Optional.of(store(userId)));
        // Bypass the cache wrapper and execute the supplied loader directly.
        when(cacheService.getOrLoad(anyString(), any(Duration.class),
                eq(InventoryStatsResponse.class), any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get());

        when(inventoryRepo.countByStoreIdAndIsActiveTrue(storeId)).thenReturn(5L);
        when(inventoryRepo.getTotalValueByStoreId(storeId)).thenReturn(new BigDecimal("1000"));
        when(inventoryRepo.countLowStockByStoreId(storeId)).thenReturn(2L);
        when(inventoryRepo.countExpiringByStoreId(eq(storeId), any(LocalDate.class))).thenReturn(1L);

        InventoryStatsResponse stats = inventoryService.getInventoryStats(storeId, userId);

        assertThat(stats.getTotalItems()).isEqualTo(5L);
        assertThat(stats.getTotalValue()).isEqualByComparingTo("1000");
        assertThat(stats.getLowStockCount()).isEqualTo(2L);
        assertThat(stats.getExpiringCount()).isEqualTo(1L);
    }
}
