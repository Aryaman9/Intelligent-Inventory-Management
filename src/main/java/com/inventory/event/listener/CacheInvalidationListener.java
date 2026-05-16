package com.inventory.event.listener;

import com.inventory.cache.CacheService;
import com.inventory.event.InventoryUpdatedEvent;
import com.inventory.event.SaleCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {

    private final CacheService cacheService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleInventoryUpdate(InventoryUpdatedEvent event) {
        cacheService.evictPattern("inventory:store:" + event.getStoreId() + "*");
        cacheService.evictPattern("stats:store:" + event.getStoreId() + "*");
        log.debug("Cache invalidated for store={}", event.getStoreId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleSaleCompleted(SaleCompletedEvent event) {
        cacheService.evictPattern("stats:store:" + event.getStoreId() + "*");
    }
}
