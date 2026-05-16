package com.inventory.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class UserActionAuditEvent {
    private final UUID userId;
    private final String action;       // e.g. "SALE_RECORDED", "USER_LOGIN", "STORE_CREATED"
    private final String resourceType; // e.g. "Transaction", "Store", "User"
    private final String resourceId;
    private final String ipAddress;
    private final String userAgent;
    private final String correlationId;
    private final Map<String, Object> metadata;
}
