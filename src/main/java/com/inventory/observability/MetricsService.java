package com.inventory.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry registry;

    public void incrementSaleSuccess(String storeId) {
        registry.counter("transaction.sale.success", "storeId", storeId).increment();
    }

    public void incrementSaleFailure(String storeId, String reason) {
        registry.counter("transaction.sale.failure", "storeId", storeId, "reason", reason).increment();
    }

    public Timer.Sample startSaleTimer() {
        return Timer.start(registry);
    }

    public void stopSaleTimer(Timer.Sample sample, String storeId) {
        sample.stop(registry.timer("transaction.sale.latency", "storeId", storeId));
    }

    public void incrementLowStockAlert(String storeId) {
        registry.counter("inventory.alert.low_stock", "storeId", storeId).increment();
    }

    public void incrementExpiryAlert(String storeId) {
        registry.counter("inventory.alert.expiring", "storeId", storeId).increment();
    }

    public void incrementCacheHit(String cacheName) {
        registry.counter("cache.hit", "cacheName", cacheName).increment();
    }

    public void incrementCacheMiss(String cacheName) {
        registry.counter("cache.miss", "cacheName", cacheName).increment();
    }

    public void incrementIdempotencyHit(String endpoint) {
        registry.counter("idempotency.hit", "endpoint", endpoint).increment();
    }

    public void incrementRateLimitExceeded(String endpoint, String tier) {
        registry.counter("ratelimit.exceeded", "endpoint", endpoint, "tier", tier).increment();
    }
}
