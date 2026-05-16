package com.inventory.repository.jpa;

import com.inventory.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    Page<Store> findByUserIdAndIsActiveTrue(UUID userId, Pageable pageable);

    Page<Store> findByUserIdAndIsActiveTrueAndNameContainingIgnoreCase(UUID userId, String name, Pageable pageable);

    long countByUserIdAndIsActiveTrue(UUID userId);

    long countByUserIdAndIsActiveFalse(UUID userId);

    List<Store> findByUserId(UUID userId);
}
