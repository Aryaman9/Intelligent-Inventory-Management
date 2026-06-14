package com.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

/**
 * Base class for all integration tests.
 *
 * <p>Boots the full application on a random port against the Testcontainers-backed Postgres,
 * MongoDB, and Redis, and exposes small RestAssured helpers for the common setup steps
 * (register a user, create a store/product, add inventory). Endpoint paths and request/response
 * shapes mirror the real controllers and DTOs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Binds the Testcontainers connection details into Spring. Referencing the static container
     * fields here triggers {@link TestContainersConfig}'s static initializer (which starts them),
     * and these values override the placeholders in {@code application.yml}.
     */
    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainersConfig.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainersConfig.POSTGRES::getUsername);
        registry.add("spring.datasource.password", TestContainersConfig.POSTGRES::getPassword);

        registry.add("spring.data.mongodb.uri", TestContainersConfig.MONGO::getReplicaSetUrl);

        registry.add("spring.data.redis.host", TestContainersConfig.REDIS::getHost);
        registry.add("spring.data.redis.port", () -> TestContainersConfig.REDIS.getMappedPort(6379));
    }

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    /** Register a user and return the raw response (HTTP 201 on success). */
    protected Response register(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "fullName", "Test User"))
                .when()
                .post("/auth/register")
                .then()
                .statusCode(201)
                .extract().response();
    }

    /** Register a user and return the access token. */
    protected String registerAndGetToken(String email, String password) {
        return register(email, password).jsonPath().getString("data.accessToken");
    }

    /** Login and return the access token. */
    protected String loginAndGetToken(String email, String password) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract().response()
                .jsonPath().getString("data.accessToken");
    }

    /** Create a store and return its ID. */
    protected String createStore(String token, String name) {
        return RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "type", "KIRANA",
                        "address", "123 Test Street",
                        "city", "Mumbai",
                        "state", "Maharashtra",
                        "pincode", "400001"))
                .when()
                .post("/stores")
                .then()
                .statusCode(201)
                .extract().response()
                .jsonPath().getString("data.id");
    }

    /** Create a product (MongoDB) and return its ID. */
    protected String createProduct(String token, String name) {
        return RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", name,
                        "category", "Grocery",
                        "brand", "Test Brand"))
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .extract().response()
                .jsonPath().getString("data.id");
    }

    /** Add inventory for a store/product and return its ID. */
    protected String addInventory(String token, String storeId, String productId,
                                  double quantity, double threshold) {
        return RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "storeId", storeId,
                        "productId", productId,
                        "quantity", quantity,
                        "costPrice", 15.00,
                        "sellingPrice", 20.00,
                        "lowStockThreshold", threshold,
                        "unit", "piece"))
                .when()
                .post("/inventory")
                .then()
                .statusCode(201)
                .extract().response()
                .jsonPath().getString("data.id");
    }
}
