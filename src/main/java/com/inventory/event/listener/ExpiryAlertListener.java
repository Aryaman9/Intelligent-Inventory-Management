package com.inventory.event.listener;

import com.inventory.event.InventoryUpdatedEvent;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryAlertListener {

    private final RedisTemplate<String, Object> redis;
    private final MetricsService metricsService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(InventoryUpdatedEvent event) {
        if (event.getExpiryDate() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), event.getExpiryDate());
            if (daysUntilExpiry >= 0 && daysUntilExpiry <= 7) {
                String key = "alert:expiring:" + event.getInventoryId();
                Map<String, Object> alert = Map.of(
                        "inventoryId", event.getInventoryId().toString(),
                        "storeId", event.getStoreId().toString(),
                        "productId", event.getProductId(),
                        "expiryDate", event.getExpiryDate().toString(),
                        "daysUntilExpiry", daysUntilExpiry,
                        "detectedAt", Instant.now().toString()
                );
                redis.opsForValue().set(key, alert, Duration.ofMinutes(30));
                metricsService.incrementExpiryAlert(event.getStoreId().toString());
                log.info("Expiry alert for inventory={}, expires in {} days",
                        event.getInventoryId(), daysUntilExpiry);
            }
        }
    }
}
