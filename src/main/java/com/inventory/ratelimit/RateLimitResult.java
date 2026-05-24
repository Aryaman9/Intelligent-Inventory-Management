package com.inventory.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {

    private final boolean allowed;
    private final long limit;
    private final long remaining;
    private final long resetInSeconds;

    public static RateLimitResult allowed(long limit, long remaining, long resetInSeconds) {
        return new RateLimitResult(true, limit, remaining, resetInSeconds);
    }

    public static RateLimitResult exceeded(long limit, long remaining, long resetInSeconds) {
        return new RateLimitResult(false, limit, remaining, resetInSeconds);
    }
}
