package com.inventory.event.listener;

import com.inventory.event.SaleCompletedEvent;
import com.inventory.observability.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockAlertListener {

    private final RedisTemplate<String, Object> redis;
    private final MetricsService metricsService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(SaleCompletedEvent event) {
        if (event.getNewQuantity().compareTo(event.getLowStockThreshold()) <= 0) {
            String key = "alert:low_stock:" + event.getInventoryId();
            Map<String, Object> alert = Map.of(
                    "inventoryId", event.getInventoryId().toString(),
                    "storeId", event.getStoreId().toString(),
                    "productId", event.getProductId(),
                    "currentQuantity", event.getNewQuantity(),
                    "threshold", event.getLowStockThreshold(),
                    "shortage", event.getLowStockThreshold().subtract(event.getNewQuantity()),
                    "detectedAt", Instant.now().toString()
            );
            redis.opsForValue().set(key, alert, Duration.ofMinutes(30));
            metricsService.incrementLowStockAlert(event.getStoreId().toString());
            log.info("Low stock alert raised for inventory={}, quantity={}",
                    event.getInventoryId(), event.getNewQuantity());
        }
    }
}
