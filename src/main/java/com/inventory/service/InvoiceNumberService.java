package com.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceNumberService {

    private final RedisTemplate<String, Object> redisTemplate;

    public String generate(UUID storeId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String key = "seq:invoice:" + storeId + ":" + date;

        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            // First invoice of the day — set TTL to expire with a buffer past midnight
            redisTemplate.expire(key, Duration.ofHours(36));
        }

        return String.format("INV%s%05d", date, seq != null ? seq : 1L);
    }
}
