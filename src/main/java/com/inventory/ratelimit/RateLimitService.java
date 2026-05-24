package com.inventory.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RateLimitProperties properties;

    public RateLimitResult tryConsume(String userId, String plan, boolean isWrite) {
        if (!properties.isEnabled()) {
            return RateLimitResult.allowed(0, 0, 0);
        }

        RateLimitProperties.TierConfig tier = properties.getTiers()
                .getOrDefault(plan, properties.getTiers().get("FREE"));

        int limit = isWrite ? tier.getWritePerMin() : tier.getReadPerMin();
        String key = "ratelimit:" + userId + ":" + (isWrite ? "write" : "read");

        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        long remaining = Math.max(0, limit - (current != null ? current : 0));
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long resetInSeconds = ttl != null && ttl > 0 ? ttl : 60;

        if (current != null && current > limit) {
            return RateLimitResult.exceeded(limit, 0, resetInSeconds);
        }

        return RateLimitResult.allowed(limit, remaining, resetInSeconds);
    }
}
