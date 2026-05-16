package com.inventory.resilience;

import com.inventory.cache.CacheService;
import com.inventory.document.Product;
import com.inventory.repository.mongo.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MongoProductClient {

    private final ProductRepository productRepo;
    private final CacheService cacheService;

    @CircuitBreaker(name = "mongoProducts", fallbackMethod = "getByIdFallback")
    public Optional<Product> getById(String productId) {
        return productRepo.findById(productId);
    }

    @CircuitBreaker(name = "mongoProducts", fallbackMethod = "getByIdsFallback")
    public List<Product> getByIds(List<String> productIds) {
        return productRepo.findAllById(productIds);
    }

    private Optional<Product> getByIdFallback(String productId, Throwable t) {
        log.warn("MongoDB circuit breaker open for product={}, using cache fallback", productId, t.getMessage());
        return cacheService.get("product:" + productId, Product.class);
    }

    private List<Product> getByIdsFallback(List<String> productIds, Throwable t) {
        log.warn("MongoDB circuit breaker open for batch product lookup, returning empty list: {}", t.getMessage());
        return List.of();
    }
}
