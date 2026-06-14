package com.inventory.integration;

import com.inventory.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves the alert pipeline: recording a sale that pushes stock below its threshold surfaces a
 * low-stock alert. The sale publishes an after-commit event consumed asynchronously, so the
 * assertion is wrapped in Awaitility to tolerate the small processing delay.
 */
class AlertPipelineIntegrationTest extends BaseIntegrationTest {

    @Test
    void saleBelowThresholdTriggersLowStockAlert() {
        String token = registerAndGetToken("alert@test.com", "password123");
        String storeId = createStore(token, "Alert Store");
        String productId = createProduct(token, "Alert Product");
        // Inventory with quantity = 12, threshold = 10.
        String inventoryId = addInventory(token, storeId, productId, 12.0, 10.0);

        // Sell 5 units -> quantity drops to 7, below the threshold of 10.
        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "inventoryId", inventoryId,
                        "quantity", 5,
                        "pricePerUnit", 20.00))
                .when()
                .post("/transactions/sale")
                .then()
                .statusCode(201);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var alertResponse = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/inventory/alerts")
                    .then()
                    .statusCode(200)
                    .extract().response();

            int lowStockCount = alertResponse.jsonPath().getInt("data.summary.lowStockCount");
            assertThat(lowStockCount).isGreaterThanOrEqualTo(1);
        });
    }
}
