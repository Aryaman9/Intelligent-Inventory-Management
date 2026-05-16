package com.inventory.exception;

public enum ErrorCode {

    // Auth
    AUTH_001("Invalid credentials"),
    AUTH_002("Account locked"),
    AUTH_003("Token expired"),
    AUTH_004("Token invalid"),
    AUTH_005("Email already exists"),

    // Resource
    RES_001("Resource not found"),

    // Inventory
    INV_001("Inventory not found"),
    INV_002("Duplicate inventory entry"),
    INV_003("Insufficient stock"),

    // Validation
    VAL_001("Validation failed"),

    // Conflict
    CONF_001("Concurrent modification"),

    // Rate limit
    RATE_001("Rate limit exceeded"),

    // General
    GEN_001("Internal server error"),
    GEN_002("Forbidden"),
    GEN_003("Bad request");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
