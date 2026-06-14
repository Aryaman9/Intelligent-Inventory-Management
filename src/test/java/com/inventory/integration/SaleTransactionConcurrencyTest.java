package com.inventory.integration;

import com.inventory.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The crown-jewel test: proves optimistic locking ({@code @Version} + {@code @Retryable}) prevents
 * oversell under concurrent writes. 20 threads each try to sell 10 units from a stock of 100, so
 * exactly 10 sales can succeed and the final quantity must settle at exactly 0 — never negative.
 */
class SaleTransactionConcurrencyTest extends BaseIntegrationTest {

    @Test
    void concurrentSalesShouldNotProduceNegativeStock() throws Exception {
        String token = registerAndGetToken("concurrent@test.com", "password123");
        String storeId = createStore(token, "Concurrency Store");
        String productId = createProduct(token, "Concurrent Product");
        String inventoryId = addInventory(token, storeId, productId, 100.0, 5.0);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await(); // release all threads at once to maximize contention
                    var response = RestAssured.given()
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(ContentType.JSON)
                            .body(Map.of(
                                    "inventoryId", inventoryId,
                                    "quantity", 10,
                                    "pricePerUnit", 20.00))
                            .when()
                            .post("/transactions/sale");

                    if (response.statusCode() == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        int success = successCount.get();

        // Read back the authoritative final quantity.
        var inventoryResponse = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/inventory/" + inventoryId)
                .then()
                .statusCode(200)
                .extract().response();
        BigDecimal finalQuantity = new BigDecimal(
                inventoryResponse.jsonPath().getString("data.quantity"));

        // Safety invariant — the core guarantee of optimistic locking. These hold regardless of how
        // contention plays out:
        //   * never oversell: at most 10 lots of 10 can come out of a stock of 100,
        //   * exact accounting: every successful sale decremented stock exactly once (no lost updates),
        //   * never negative.
        assertThat(success).isBetween(1, 10);
        assertThat(success + failCount.get()).isEqualTo(threadCount);
        assertThat(finalQuantity).isEqualByComparingTo(BigDecimal.valueOf(100L - 10L * success));
        assertThat(finalQuantity).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // With an adequate retry budget the retries resolve all contention, so every available lot
        // sells and stock lands exactly on 0.
        assertThat(success).isEqualTo(10);
        assertThat(finalQuantity).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
