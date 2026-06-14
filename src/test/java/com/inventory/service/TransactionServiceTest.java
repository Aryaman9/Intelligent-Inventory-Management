package com.inventory.service;

import com.inventory.document.Product;
import com.inventory.dto.request.SaleRequest;
import com.inventory.dto.response.SaleResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.entity.Inventory;
import com.inventory.entity.Store;
import com.inventory.entity.Transaction;
import com.inventory.entity.User;
import com.inventory.exception.ForbiddenException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.idempotency.IdempotencyStore;
import com.inventory.mapper.TransactionMapper;
import com.inventory.observability.MetricsService;
import com.inventory.repository.jpa.InventoryRepository;
import com.inventory.repository.jpa.StoreRepository;
import com.inventory.repository.jpa.TransactionRepository;
import com.inventory.resilience.MongoProductClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private InventoryRepository inventoryRepo;
    @Mock private TransactionRepository txnRepo;
    @Mock private StoreRepository storeRepo;
    @Mock private MongoProductClient mongoProductClient;
    @Mock private InvoiceNumberService invoiceService;
    @Mock private IdempotencyStore idempotencyStore;
    @Mock private TransactionMapper txnMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    // Real metrics service backed by an in-memory registry — avoids brittle Timer.Sample stubbing.
    private final MetricsService metricsService = new MetricsService(new SimpleMeterRegistry());

    private TransactionService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID inventoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransactionService(
                inventoryRepo, txnRepo, storeRepo, mongoProductClient,
                invoiceService, idempotencyStore, txnMapper, eventPublisher, metricsService);
        // recordSale registers an afterCommit synchronization; activate the manager so it doesn't throw.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Inventory inventory(UUID ownerId, BigDecimal quantity) {
        User owner = User.builder().email("o@test.com").passwordHash("h").fullName("Owner").build();
        owner.setId(ownerId);
        Store store = Store.builder()
                .user(owner).name("S").type("KIRANA").address("a")
                .city("c").state("st").pincode("400001").build();
        store.setId(storeId);
        Inventory inv = Inventory.builder()
                .store(store).productId("p1")
                .quantity(quantity)
                .lowStockThreshold(new BigDecimal("5"))
                .costPrice(new BigDecimal("15"))
                .sellingPrice(new BigDecimal("20"))
                .build();
        inv.setId(inventoryId);
        return inv;
    }

    private SaleRequest saleRequest(BigDecimal quantity) {
        return SaleRequest.builder()
                .inventoryId(inventoryId)
                .quantity(quantity)
                .pricePerUnit(new BigDecimal("20"))
                .build();
    }

    @Test
    void recordSale_happyPath_decrementsStockAndReturnsResponse() {
        Inventory inv = inventory(userId, new BigDecimal("100"));

        when(idempotencyStore.get("sale", "key-1", SaleResponse.class)).thenReturn(Optional.empty());
        when(inventoryRepo.findByIdWithStoreAndUser(inventoryId)).thenReturn(Optional.of(inv));
        when(mongoProductClient.getById("p1"))
                .thenReturn(Optional.of(Product.builder().id("p1").name("Widget").build()));
        when(inventoryRepo.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
        when(invoiceService.generate(storeId)).thenReturn("INV-001");
        when(txnRepo.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(txnMapper.toResponse(any(Transaction.class)))
                .thenReturn(TransactionResponse.builder().id(UUID.randomUUID()).build());

        SaleResponse response = service.recordSale(saleRequest(new BigDecimal("10")), userId, "key-1");

        assertThat(response.getInventoryQuantity()).isEqualByComparingTo("90");
        assertThat(response.getProductName()).isEqualTo("Widget");
        verify(inventoryRepo).save(inv);
        verify(txnRepo).save(any(Transaction.class));
    }

    @Test
    void recordSale_insufficientStock_throws() {
        Inventory inv = inventory(userId, new BigDecimal("5"));
        when(idempotencyStore.get("sale", "key-2", SaleResponse.class)).thenReturn(Optional.empty());
        when(inventoryRepo.findByIdWithStoreAndUser(inventoryId)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> service.recordSale(saleRequest(new BigDecimal("10")), userId, "key-2"))
                .isInstanceOf(InsufficientStockException.class);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    void recordSale_notOwner_throwsForbidden() {
        Inventory inv = inventory(UUID.randomUUID(), new BigDecimal("100"));
        when(idempotencyStore.get("sale", "key-3", SaleResponse.class)).thenReturn(Optional.empty());
        when(inventoryRepo.findByIdWithStoreAndUser(inventoryId)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> service.recordSale(saleRequest(new BigDecimal("10")), userId, "key-3"))
                .isInstanceOf(ForbiddenException.class);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    void recordSale_idempotentReplay_returnsCachedWithoutTouchingInventory() {
        SaleResponse cached = SaleResponse.builder()
                .productName("cached-product")
                .inventoryQuantity(new BigDecimal("42"))
                .build();
        when(idempotencyStore.get("sale", "key-4", SaleResponse.class)).thenReturn(Optional.of(cached));

        SaleResponse response = service.recordSale(saleRequest(new BigDecimal("10")), userId, "key-4");

        assertThat(response).isSameAs(cached);
        verify(inventoryRepo, never()).findByIdWithStoreAndUser(any());
        verify(inventoryRepo, never()).save(any());
    }
}
