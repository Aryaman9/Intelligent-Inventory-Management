package com.inventory;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies the full Spring application context boots against the Testcontainers-backed
 * Postgres, MongoDB, and Redis. Extends {@link BaseIntegrationTest} so it shares the same cached
 * context (and containers) as the other integration tests rather than spinning up its own.
 */
class InventoryApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
    }
}
