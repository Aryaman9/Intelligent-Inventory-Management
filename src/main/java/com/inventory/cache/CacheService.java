package com.inventory.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.observability.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            metricsService.incrementCacheMiss(cacheName(key));
            return Optional.empty();
        }
        try {
            metricsService.incrementCacheHit(cacheName(key));
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            return Optional.of(objectMapper.convertValue(value, type));
        } catch (Exception e) {
            log.warn("Cache deserialization failed for key={}, type={}", key, type.getSimpleName(), e);
            return Optional.empty();
        }
    }

    private String cacheName(String key) {
        int idx = key.indexOf(':');
        return idx > 0 ? key.substring(0, idx) : key;
    }

    public <T> void put(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    public void evictPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Get-or-load with stampede protection via a Redis lock.
     * On cache miss, only one thread loads from source; others poll until the cache is warm.
     */
    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) return cached.get();

        String lockKey = "lock:" + key;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

        if (Boolean.TRUE.equals(acquired)) {
            try {
                // Double-check after acquiring lock
                cached = get(key, type);
                if (cached.isPresent()) return cached.get();

                T value = loader.get();
                put(key, value, ttl);
                return value;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // Wait for the lock holder to populate the cache
            for (int i = 0; i < 4; i++) {
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                cached = get(key, type);
                if (cached.isPresent()) return cached.get();
            }
            return loader.get();
        }
    }
}
