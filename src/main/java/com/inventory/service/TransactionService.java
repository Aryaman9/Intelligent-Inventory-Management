package com.inventory.service;

import com.inventory.document.Product;
import com.inventory.dto.request.PurchaseRequest;
import com.inventory.dto.request.ReturnRequest;
import com.inventory.dto.request.SaleRequest;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.dto.response.SaleResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.dto.response.TransactionStatsResponse;
import com.inventory.entity.Inventory;
import com.inventory.entity.Transaction;
import com.inventory.entity.TransactionType;
import com.inventory.event.InventoryUpdatedEvent;
import com.inventory.event.PurchaseCompletedEvent;
import com.inventory.event.SaleCompletedEvent;
import com.inventory.event.UserActionAuditEvent;
import com.inventory.exception.ConflictException;
import com.inventory.exception.ErrorCode;
import com.inventory.exception.ForbiddenException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.idempotency.IdempotencyStore;
import com.inventory.mapper.TransactionMapper;
import com.inventory.repository.jpa.InventoryRepository;
import com.inventory.repository.jpa.StoreRepository;
import com.inventory.repository.jpa.TransactionRepository;
import com.inventory.resilience.MongoProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final InventoryRepository inventoryRepo;
    private final TransactionRepository txnRepo;
    private final StoreRepository storeRepo;
    private final MongoProductClient mongoProductClient;
    private final InvoiceNumberService invoiceService;
    private final IdempotencyStore idempotencyStore;
    private final TransactionMapper txnMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public SaleResponse recordSale(SaleRequest request, UUID userId, String idempotencyKey) {

        Optional<SaleResponse> cached = idempotencyStore.get("sale", idempotencyKey, SaleResponse.class);
        if (cached.isPresent()) {
            log.info("Idempotent replay for sale key={}", idempotencyKey);
            return cached.get();
        }

        Inventory inv = inventoryRepo.findByIdWithStoreAndUser(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (!inv.getStore().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this inventory");
        }

        if (inv.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new InsufficientStockException(
                    "Insufficient stock: requested " + request.getQuantity()
                            + ", available " + inv.getQuantity());
        }

        String productName = mongoProductClient.getById(inv.getProductId())
                .map(Product::getName).orElse("Unknown Product");

        inv.setQuantity(inv.getQuantity().subtract(request.getQuantity()));
        inv.setLastSoldAt(LocalDateTime.now());
        inventoryRepo.save(inv);

        String invoiceNumber = invoiceService.generate(inv.getStore().getId());

        Transaction txn = Transaction.builder()
                .store(inv.getStore())
                .inventory(inv)
                .type(TransactionType.SALE)
                .quantity(request.getQuantity())
                .pricePerUnit(request.getPricePerUnit())
                .totalAmount(request.getQuantity().multiply(request.getPricePerUnit()))
                .invoiceNumber(invoiceNumber)
                .idempotencyKey(idempotencyKey)
                .paymentMethod(request.getPaymentMethod())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .notes(request.getNotes())
                .build();
        txnRepo.save(txn);

        TransactionResponse txnResponse = txnMapper.toResponse(txn);
        txnResponse.setProductName(productName);

        SaleResponse response = SaleResponse.builder()
                .transaction(txnResponse)
                .inventoryQuantity(inv.getQuantity())
                .productName(productName)
                .build();

        // Capture for lambda (inv is already effectively final after save returns the same object)
        final Inventory savedInv = inv;
        final Transaction savedTxn = txn;
        final String correlationId = MDC.get("traceId");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                idempotencyStore.put("sale", idempotencyKey, response, Duration.ofHours(24));

                eventPublisher.publishEvent(new SaleCompletedEvent(
                        savedTxn.getId(), savedInv.getId(), savedInv.getStore().getId(),
                        savedInv.getQuantity(), savedInv.getLowStockThreshold(),
                        savedInv.getExpiryDate(), savedInv.getProductId(),
                        savedTxn.getTotalAmount()
                ));

                eventPublisher.publishEvent(new InventoryUpdatedEvent(
                        savedInv.getId(), savedInv.getStore().getId(), savedInv.getQuantity(),
                        savedInv.getLowStockThreshold(), savedInv.getExpiryDate(),
                        savedInv.getProductId(), "SALE"
                ));

                eventPublisher.publishEvent(UserActionAuditEvent.builder()
                        .userId(userId)
                        .action("SALE_RECORDED")
                        .resourceType("Transaction")
                        .resourceId(savedTxn.getId().toString())
                        .correlationId(correlationId)
                        .metadata(Map.of(
                                "amount", savedTxn.getTotalAmount(),
                                "invoiceNumber", savedTxn.getInvoiceNumber()))
                        .build());
            }
        });

        return response;
    }

    @Recover
    public SaleResponse recoverSale(Throwable ex,
                                    SaleRequest request, UUID userId, String idempotencyKey) {
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            log.error("Sale failed after 3 retries due to optimistic lock conflict", ex);
            throw new ConflictException(
                    "Transaction failed due to concurrent modification — please retry",
                    ErrorCode.CONF_001);
        }
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public SaleResponse recordPurchase(PurchaseRequest request, UUID userId, String idempotencyKey) {

        Optional<SaleResponse> cached = idempotencyStore.get("purchase", idempotencyKey, SaleResponse.class);
        if (cached.isPresent()) {
            log.info("Idempotent replay for purchase key={}", idempotencyKey);
            return cached.get();
        }

        Inventory inv = inventoryRepo.findByIdWithStoreAndUser(request.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (!inv.getStore().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this inventory");
        }

        String productName = mongoProductClient.getById(inv.getProductId())
                .map(Product::getName).orElse("Unknown Product");

        inv.setQuantity(inv.getQuantity().add(request.getQuantity()));
        inv.setCostPrice(request.getPricePerUnit());
        inv.setLastRestockedAt(LocalDateTime.now());
        inventoryRepo.save(inv);

        String invoiceNumber = invoiceService.generate(inv.getStore().getId());

        Transaction txn = Transaction.builder()
                .store(inv.getStore())
                .inventory(inv)
                .type(TransactionType.PURCHASE)
                .quantity(request.getQuantity())
                .pricePerUnit(request.getPricePerUnit())
                .totalAmount(request.getQuantity().multiply(request.getPricePerUnit()))
                .invoiceNumber(invoiceNumber)
                .idempotencyKey(idempotencyKey)
                .notes(request.getNotes())
                .build();
        txnRepo.save(txn);

        TransactionResponse txnResponse = txnMapper.toResponse(txn);
        txnResponse.setProductName(productName);

        SaleResponse response = SaleResponse.builder()
                .transaction(txnResponse)
                .inventoryQuantity(inv.getQuantity())
                .productName(productName)
                .build();

        final Inventory savedInv = inv;
        final Transaction savedTxn = txn;
        final String correlationId = MDC.get("traceId");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                idempotencyStore.put("purchase", idempotencyKey, response, Duration.ofHours(24));

                eventPublisher.publishEvent(new PurchaseCompletedEvent(
                        savedTxn.getId(), savedInv.getId(), savedInv.getStore().getId(),
                        savedInv.getQuantity(), savedInv.getLowStockThreshold(),
                        savedInv.getExpiryDate(), savedInv.getProductId(),
                        savedTxn.getTotalAmount()
                ));

                eventPublisher.publishEvent(new InventoryUpdatedEvent(
                        savedInv.getId(), savedInv.getStore().getId(), savedInv.getQuantity(),
                        savedInv.getLowStockThreshold(), savedInv.getExpiryDate(),
                        savedInv.getProductId(), "PURCHASE"
                ));

                eventPublisher.publishEvent(UserActionAuditEvent.builder()
                        .userId(userId)
                        .action("PURCHASE_RECORDED")
                        .resourceType("Transaction")
                        .resourceId(savedTxn.getId().toString())
                        .correlationId(correlationId)
                        .metadata(Map.of(
                                "amount", savedTxn.getTotalAmount(),
                                "invoiceNumber", savedTxn.getInvoiceNumber()))
                        .build());
            }
        });

        return response;
    }

    @Recover
    public SaleResponse recoverPurchase(Throwable ex,
                                        PurchaseRequest request, UUID userId, String idempotencyKey) {
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            log.error("Purchase failed after 3 retries due to optimistic lock conflict", ex);
            throw new ConflictException(
                    "Transaction failed due to concurrent modification — please retry",
                    ErrorCode.CONF_001);
        }
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public TransactionResponse recordReturn(ReturnRequest request, UUID userId, String idempotencyKey) {

        Optional<TransactionResponse> cached =
                idempotencyStore.get("return", idempotencyKey, TransactionResponse.class);
        if (cached.isPresent()) {
            log.info("Idempotent replay for return key={}", idempotencyKey);
            return cached.get();
        }

        Transaction original = txnRepo.findById(request.getOriginalTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Original transaction not found"));

        if (original.getType() != TransactionType.SALE) {
            throw new com.inventory.exception.BadRequestException("Only SALE transactions can be returned");
        }

        Inventory inv = inventoryRepo.findByIdWithStoreAndUser(original.getInventory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (!inv.getStore().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this inventory");
        }

        if (request.getQuantity().compareTo(original.getQuantity()) > 0) {
            throw new com.inventory.exception.BadRequestException(
                    "Return quantity cannot exceed original sale quantity of " + original.getQuantity());
        }

        String productName = mongoProductClient.getById(inv.getProductId())
                .map(Product::getName).orElse("Unknown Product");

        inv.setQuantity(inv.getQuantity().add(request.getQuantity()));
        inventoryRepo.save(inv);

        String invoiceNumber = invoiceService.generate(inv.getStore().getId());
        String notes = "Return for invoice " + original.getInvoiceNumber()
                + (request.getReason() != null ? " — " + request.getReason() : "")
                + (request.getNotes() != null ? ". " + request.getNotes() : "");

        Transaction txn = Transaction.builder()
                .store(inv.getStore())
                .inventory(inv)
                .type(TransactionType.RETURN)
                .quantity(request.getQuantity())
                .pricePerUnit(original.getPricePerUnit())
                .totalAmount(request.getQuantity().multiply(original.getPricePerUnit()))
                .invoiceNumber(invoiceNumber)
                .idempotencyKey(idempotencyKey)
                .notes(notes)
                .build();
        txnRepo.save(txn);

        TransactionResponse response = txnMapper.toResponse(txn);
        response.setProductName(productName);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                idempotencyStore.put("return", idempotencyKey, response, Duration.ofHours(24));
            }
        });

        return response;
    }

    @Recover
    public TransactionResponse recoverReturn(Throwable ex,
                                             ReturnRequest request, UUID userId, String idempotencyKey) {
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            log.error("Return failed after 3 retries due to optimistic lock conflict", ex);
            throw new ConflictException(
                    "Transaction failed due to concurrent modification — please retry",
                    ErrorCode.CONF_001);
        }
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<TransactionResponse> getStoreTransactions(
            UUID storeId, UUID userId, String type, LocalDate startDate, LocalDate endDate,
            int page, int limit) {

        verifyStoreOwnership(storeId, userId);

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Transaction> txnPage;

        TransactionType txnType = type != null ? TransactionType.valueOf(type.toUpperCase()) : null;
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;

        if (txnType != null && start != null) {
            txnPage = txnRepo.findByStoreIdAndTypeAndCreatedAtBetween(storeId, txnType, start, end, pageable);
        } else if (txnType != null) {
            txnPage = txnRepo.findByStoreIdAndType(storeId, txnType, pageable);
        } else if (start != null) {
            txnPage = txnRepo.findByStoreIdAndCreatedAtBetween(storeId, start, end, pageable);
        } else {
            txnPage = txnRepo.findByStoreId(storeId, pageable);
        }

        // Batch-fetch product names
        List<String> productIds = txnPage.getContent().stream()
                .map(t -> t.getInventory().getProductId())
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> productNameMap = new HashMap<>();
        mongoProductClient.getByIds(productIds)
                .forEach(p -> productNameMap.put(p.getId(), p.getName()));

        List<TransactionResponse> items = txnPage.getContent().stream()
                .map(t -> {
                    TransactionResponse r = txnMapper.toResponse(t);
                    r.setProductName(productNameMap.getOrDefault(t.getInventory().getProductId(), "Unknown"));
                    return r;
                })
                .collect(Collectors.toList());

        return PaginatedResponse.<TransactionResponse>builder()
                .items(items)
                .pagination(PaginatedResponse.PaginationInfo.builder()
                        .total(txnPage.getTotalElements())
                        .page(page)
                        .limit(limit)
                        .pages(txnPage.getTotalPages())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID txnId, UUID userId) {
        Transaction txn = txnRepo.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        verifyStoreOwnership(txn.getStore().getId(), userId);
        String productName = mongoProductClient.getById(txn.getInventory().getProductId())
                .map(Product::getName).orElse("Unknown Product");
        TransactionResponse response = txnMapper.toResponse(txn);
        response.setProductName(productName);
        return response;
    }

    @Transactional(readOnly = true)
    public TransactionStatsResponse getTransactionStats(
            UUID storeId, UUID userId, LocalDate startDate, LocalDate endDate) {

        verifyStoreOwnership(storeId, userId);

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        List<Transaction> allTxns = txnRepo.findByStoreIdAndCreatedAtBetween(storeId, start, end);

        List<Transaction> sales = allTxns.stream()
                .filter(t -> t.getType() == TransactionType.SALE)
                .collect(Collectors.toList());
        List<Transaction> purchases = allTxns.stream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .collect(Collectors.toList());

        BigDecimal totalSalesAmount = sales.stream()
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Batch-fetch inventory cost prices for profit calculation
        Set<UUID> inventoryIds = sales.stream()
                .map(t -> t.getInventory().getId())
                .collect(Collectors.toSet());
        Map<UUID, BigDecimal> costPriceMap = inventoryRepo.findAllById(inventoryIds).stream()
                .collect(Collectors.toMap(com.inventory.entity.BaseEntity::getId, Inventory::getCostPrice));

        BigDecimal totalProfit = sales.stream()
                .map(t -> t.getPricePerUnit()
                        .subtract(costPriceMap.getOrDefault(t.getInventory().getId(), BigDecimal.ZERO))
                        .multiply(t.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitMargin = totalSalesAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalSalesAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Payment method breakdown (for sales)
        Map<String, Long> paymentMethodBreakdown = sales.stream()
                .filter(t -> t.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(Transaction::getPaymentMethod, Collectors.counting()));

        // Daily revenue
        Map<String, BigDecimal> dailyMap = sales.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getTotalAmount, BigDecimal::add)));

        List<TransactionStatsResponse.DailyRevenue> dailyRevenue = dailyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> TransactionStatsResponse.DailyRevenue.builder()
                        .date(e.getKey())
                        .revenue(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return TransactionStatsResponse.builder()
                .totalTransactions((long) allTxns.size())
                .totalSalesCount((long) sales.size())
                .totalPurchasesCount((long) purchases.size())
                .totalSalesAmount(totalSalesAmount)
                .totalRevenue(totalSalesAmount)
                .totalProfit(totalProfit)
                .profitMargin(profitMargin)
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .dailyRevenue(dailyRevenue)
                .build();
    }

    private void verifyStoreOwnership(UUID storeId, UUID userId) {
        com.inventory.entity.Store store = storeRepo.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        if (!store.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this store");
        }
    }
}
