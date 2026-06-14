package com.inventory.integration;

import com.inventory.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the full authentication lifecycle: register, login, token-protected access, logout
 * (with immediate blacklist), account lockout after repeated failures, and refresh-token rotation.
 */
class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void completeAuthFlow() {
        String email = "authflow@test.com";
        String password = "password123";

        // 1. Register
        register(email, password);

        // 2. Login with correct password -> success, capture token
        String token = loginAndGetToken(email, password);
        assertThat(token).isNotBlank();

        // 3. GET /me with token -> returns the user
        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/auth/me")
                .then().statusCode(200)
                .body("data.email", org.hamcrest.Matchers.equalTo(email));

        // 4. Logout with the access token -> success. Send a JSON content-type so the optional
        //    JSON @RequestBody endpoint doesn't reject the default form-urlencoded type.
        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when().post("/auth/logout")
                .then().statusCode(200);

        // 5. GET /me with the same (now blacklisted) token -> 401
        RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/auth/me")
                .then().statusCode(401);
    }

    @Test
    void accountLockoutAfterFailedLogins() {
        String email = "lockout@test.com";
        String password = "password123";
        register(email, password);

        // 5 failed logins. Each returns 401 "Invalid credentials"; the 5th also sets the lock.
        for (int i = 0; i < 5; i++) {
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("email", email, "password", "wrong-password"))
                    .when().post("/auth/login")
                    .then().statusCode(401);
        }

        // With the account now locked, even the correct password is rejected with a "locked" message.
        Response locked = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post("/auth/login")
                .then().statusCode(401)
                .extract().response();

        String error = locked.jsonPath().getString("error");
        assertThat(error).isNotNull();
        assertThat(error.toLowerCase()).contains("locked");
    }

    @Test
    void tokenRefreshAndRotation() {
        String email = "refresh@test.com";
        String password = "password123";

        Response registration = register(email, password);
        String oldRefreshToken = registration.jsonPath().getString("data.refreshToken");
        assertThat(oldRefreshToken).isNotBlank();

        // Refresh -> new access + refresh tokens
        Response refreshed = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", oldRefreshToken))
                .when().post("/auth/refresh")
                .then().statusCode(200)
                .extract().response();

        String newAccessToken = refreshed.jsonPath().getString("data.accessToken");
        String newRefreshToken = refreshed.jsonPath().getString("data.refreshToken");
        assertThat(newAccessToken).isNotBlank();
        assertThat(newRefreshToken).isNotBlank();

        // Reusing the OLD refresh token must fail (it was blacklisted on rotation).
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("refreshToken", oldRefreshToken))
                .when().post("/auth/refresh")
                .then().statusCode(401);

        // The NEW access token still works.
        RestAssured.given()
                .header("Authorization", "Bearer " + newAccessToken)
                .when().get("/auth/me")
                .then().statusCode(200)
                .body("data.email", org.hamcrest.Matchers.equalTo(email));
    }
}
