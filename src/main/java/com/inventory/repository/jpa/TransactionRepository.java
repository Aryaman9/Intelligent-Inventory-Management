package com.inventory.repository.jpa;

import com.inventory.entity.Transaction;
import com.inventory.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByStoreId(UUID storeId, Pageable pageable);

    Optional<Transaction> findByIdempotencyKey(String key);

    Page<Transaction> findByStoreIdAndType(UUID storeId, TransactionType type, Pageable pageable);

    Page<Transaction> findByStoreIdAndCreatedAtBetween(
            UUID storeId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Transaction> findByStoreIdAndTypeAndCreatedAtBetween(
            UUID storeId, TransactionType type, LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<Transaction> findByStoreIdAndCreatedAtBetween(
            UUID storeId, LocalDateTime start, LocalDateTime end);
}
