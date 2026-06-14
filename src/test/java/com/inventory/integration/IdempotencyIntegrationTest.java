package com.inventory.integration;

import com.inventory.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that retrying the same mutating request with the same Idempotency-Key produces exactly one
 * transaction and replays the original response payload, rather than applying the effect repeatedly.
 *
 * <p>Note: the {@code ApiResponse} envelope stamps a fresh {@code timestamp}/{@code correlationId}
 * per request, so the full HTTP bodies differ by design. The idempotency guarantee lives in the
 * {@code data} payload (same transaction id, invoice number, resulting quantity), which is what we
 * assert here, alongside the authoritative database checks.
 */
class IdempotencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void sameSaleWithSameIdempotencyKeyProducesOneTransaction() {
        String token = registerAndGetToken("idem@test.com", "password123");
        String storeId = createStore(token, "Idem Store");
        String productId = createProduct(token, "Idem Product");
        String inventoryId = addInventory(token, storeId, productId, 100.0, 5.0);
        String idempotencyKey = UUID.randomUUID().toString();

        // Send the SAME sale 5 times with the SAME idempotency key.
        List<Map<String, Object>> dataPayloads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Response response = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "inventoryId", inventoryId,
                            "quantity", 10,
                            "pricePerUnit", 20.00))
                    .when()
                    .post("/transactions/sale")
                    .then()
                    .statusCode(201)
                    .extract().response();

            dataPayloads.add(response.jsonPath().getMap("data"));
        }

        // Every replay returns the identical business payload (same transaction, same resulting state).
        Map<String, Object> first = dataPayloads.get(0);
        for (int i = 1; i < dataPayloads.size(); i++) {
            assertThat(dataPayloads.get(i)).isEqualTo(first);
        }

        // Only one transaction should exist for the store.
        Response txnList = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/transactions/store/" + storeId + "?page=1&limit=100")
                .then()
                .statusCode(200)
                .extract().response();
        assertThat(txnList.jsonPath().getInt("data.pagination.total")).isEqualTo(1);

        // Inventory decreased exactly once: 100 -> 90.
        Response inv = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/inventory/" + inventoryId)
                .then()
                .statusCode(200)
                .extract().response();
        BigDecimal qty = new BigDecimal(inv.jsonPath().getString("data.quantity"));
        assertThat(qty).isEqualByComparingTo(new BigDecimal("90"));
    }
}
