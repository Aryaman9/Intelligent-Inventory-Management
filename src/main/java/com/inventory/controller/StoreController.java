package com.inventory.controller;

import com.inventory.dto.request.CreateStoreRequest;
import com.inventory.dto.request.UpdateStoreRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.dto.response.StoreResponse;
import com.inventory.dto.response.StoreStatsResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> createStore(@Valid @RequestBody CreateStoreRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        StoreResponse store = storeService.createStore(request, userId);
        return ApiResponse.success("Store created successfully", store);
    }

    @GetMapping
    public ApiResponse<PaginatedResponse<StoreResponse>> getUserStores(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String q) {
        UUID userId = securityUtils.getCurrentUserId();
        return ApiResponse.success(storeService.getUserStores(userId, q, page, limit));
    }

    @GetMapping("/stats")
    public ApiResponse<StoreStatsResponse> getStoreStats() {
        UUID userId = securityUtils.getCurrentUserId();
        return ApiResponse.success(storeService.getStoreStats(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<StoreResponse> getStoreById(@PathVariable UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        return ApiResponse.success(storeService.getStoreById(id, userId));
    }

    @PatchMapping("/{id}")
    public ApiResponse<StoreResponse> updateStore(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStoreRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        return ApiResponse.success(storeService.updateStore(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteStore(@PathVariable UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        storeService.deleteStore(id, userId);
        return ApiResponse.success("Store deleted", null);
    }
}
