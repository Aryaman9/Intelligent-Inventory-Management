package com.inventory.integration;

import com.inventory.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves per-user, per-tier rate limiting. A FREE-tier user is allowed 30 write requests/minute;
 * issuing 35 store creates in a burst must yield at most 30 successes and at least one HTTP 429
 * carrying a Retry-After header.
 */
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Test
    void freeUserExceedingWriteRateLimitGets429() {
        String token = registerAndGetToken("ratelimit@test.com", "password123");

        int successCount = 0;
        int rateLimitedCount = 0;

        for (int i = 0; i < 35; i++) {
            Response response = RestAssured.given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(Map.of(
                            "name", "Store " + i,
                            "type", "KIRANA",
                            "address", "addr",
                            "city", "Mumbai",
                            "state", "MH",
                            "pincode", "400001"))
                    .when()
                    .post("/stores");

            if (response.statusCode() == 201) {
                successCount++;
            } else if (response.statusCode() == 429) {
                rateLimitedCount++;
                assertThat(response.header("Retry-After")).isNotNull();
            }
        }

        assertThat(successCount).isLessThanOrEqualTo(30);
        assertThat(rateLimitedCount).isGreaterThan(0);
    }
}
