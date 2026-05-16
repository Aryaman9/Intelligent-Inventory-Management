package com.inventory.repository.jpa;

import com.inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Page<Inventory> findByStoreIdAndIsActiveTrue(UUID storeId, Pageable pageable);

    Optional<Inventory> findByStoreIdAndProductId(UUID storeId, String productId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.store s JOIN FETCH s.user WHERE i.id = :id")
    Optional<Inventory> findByIdWithStoreAndUser(@Param("id") UUID id);

    @Query("SELECT i FROM Inventory i WHERE i.store.id = :storeId AND i.isActive = true AND i.quantity <= i.lowStockThreshold")
    Page<Inventory> findLowStockByStoreId(@Param("storeId") UUID storeId, Pageable pageable);

    long countByStoreIdAndIsActiveTrue(UUID storeId);

    @Query("SELECT COALESCE(SUM(i.quantity * i.costPrice), 0) FROM Inventory i WHERE i.store.id = :storeId AND i.isActive = true")
    BigDecimal getTotalValueByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.store.id = :storeId AND i.isActive = true AND i.quantity <= i.lowStockThreshold")
    long countLowStockByStoreId(@Param("storeId") UUID storeId);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.store.id = :storeId AND i.isActive = true AND i.expiryDate IS NOT NULL AND i.expiryDate <= :threshold")
    long countExpiringByStoreId(@Param("storeId") UUID storeId, @Param("threshold") LocalDate threshold);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.store WHERE i.store.id IN :storeIds AND i.isActive = true AND i.quantity <= i.lowStockThreshold")
    List<Inventory> findLowStockByStoreIds(@Param("storeIds") List<UUID> storeIds);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.store WHERE i.store.id IN :storeIds AND i.isActive = true AND i.expiryDate IS NOT NULL AND i.expiryDate <= :threshold AND i.expiryDate >= :today")
    List<Inventory> findExpiringByStoreIds(@Param("storeIds") List<UUID> storeIds, @Param("threshold") LocalDate threshold, @Param("today") LocalDate today);
}
