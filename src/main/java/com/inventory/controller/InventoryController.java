package com.inventory.controller;

import com.inventory.dto.request.CreateInventoryRequest;
import com.inventory.dto.request.UpdateInventoryRequest;
import com.inventory.dto.response.AlertsResponse;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.InventoryResponse;
import com.inventory.dto.response.InventoryStatsResponse;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        InventoryResponse response = inventoryService.createInventory(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Inventory created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(@PathVariable UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryById(id, userId)));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryResponse>>> getStoreInventory(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(name = "low_stock_only", defaultValue = "false") boolean lowStockOnly) {
        UUID userId = securityUtils.getCurrentUserId();
        PaginatedResponse<InventoryResponse> response =
                inventoryService.getStoreInventory(storeId, userId, lowStockOnly, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        InventoryResponse response = inventoryService.updateInventory(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteInventory(@PathVariable UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        inventoryService.deleteInventory(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Inventory deleted", Map.of("id", id.toString())));
    }

    @GetMapping("/stats/{storeId}")
    public ResponseEntity<ApiResponse<InventoryStatsResponse>> getInventoryStats(@PathVariable UUID storeId) {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryStats(storeId, userId)));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<AlertsResponse>> getAlerts() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAlerts(userId)));
    }
}
