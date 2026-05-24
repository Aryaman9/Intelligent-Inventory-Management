package com.inventory.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.observability.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyStore {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public <T> Optional<T> get(String scope, String key, Class<T> type) {
        String redisKey = "idem:" + scope + ":" + key;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) return Optional.empty();
        metricsService.incrementIdempotencyHit(scope);
        return Optional.of(objectMapper.convertValue(cached, type));
    }

    public <T> void put(String scope, String key, T value, Duration ttl) {
        String redisKey = "idem:" + scope + ":" + key;
        redisTemplate.opsForValue().set(redisKey, value, ttl);
    }
}
