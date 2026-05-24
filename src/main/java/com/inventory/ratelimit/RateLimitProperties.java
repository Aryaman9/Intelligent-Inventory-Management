package com.inventory.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Data
public class RateLimitProperties {

    private boolean enabled = true;
    private Map<String, TierConfig> tiers = new HashMap<>();

    @Data
    public static class TierConfig {
        private int readPerMin;
        private int writePerMin;
    }
}
