package com.inventory;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers definitions for all integration tests.
 *
 * <p>The containers are {@code static} and started once in the static initializer, so they are
 * spun up a single time per JVM and reused across every {@code @SpringBootTest} that extends
 * {@link BaseIntegrationTest} (Spring also caches the application context across those classes).
 *
 * <p>The connection details are bound to Spring via a {@code @DynamicPropertySource} method in
 * {@link BaseIntegrationTest} — declaring it on the abstract test base class is the portable,
 * always-supported location, whereas {@code @DynamicPropertySource} on an imported configuration
 * is not honored consistently across Spring versions.
 */
@TestConfiguration
public class TestContainersConfig {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:14-alpine")
                    .withDatabaseName("inventory_test")
                    .withUsername("test")
                    .withPassword("test");

    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:6");

    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        MONGO.start();
        REDIS.start();
    }
}
