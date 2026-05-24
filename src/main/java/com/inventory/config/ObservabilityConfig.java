package com.inventory.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public SmartInitializingSingleton commonTagsInitializer(MeterRegistry registry) {
        return () -> registry.config().commonTags("application", "inventory-management");
    }
}
