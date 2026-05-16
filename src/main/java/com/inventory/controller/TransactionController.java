package com.inventory.controller;

import com.inventory.dto.request.PurchaseRequest;
import com.inventory.dto.request.ReturnRequest;
import com.inventory.dto.request.SaleRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.dto.response.SaleResponse;
import com.inventory.dto.response.TransactionResponse;
import com.inventory.dto.response.TransactionStatsResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    @PostMapping("/sale")
    public ResponseEntity<ApiResponse<SaleResponse>> recordSale(
            @Valid @RequestBody SaleRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID userId = securityUtils.getCurrentUserId();
        SaleResponse response = transactionService.recordSale(request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Sale recorded", response));
    }

    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<SaleResponse>> recordPurchase(
            @Valid @RequestBody PurchaseRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID userId = securityUtils.getCurrentUserId();
        SaleResponse response = transactionService.recordPurchase(request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Purchase recorded", response));
    }

    @PostMapping("/return")
    public ResponseEntity<ApiResponse<TransactionResponse>> recordReturn(
            @Valid @RequestBody ReturnRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        UUID userId = securityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.recordReturn(request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Return recorded", response));
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<ApiResponse<PaginatedResponse<TransactionResponse>>> getStoreTransactions(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String type,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID userId = securityUtils.getCurrentUserId();
        PaginatedResponse<TransactionResponse> response =
                transactionService.getStoreTransactions(storeId, userId, type, startDate, endDate, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionById(id, userId)));
    }

    @GetMapping("/stats/{storeId}")
    public ResponseEntity<ApiResponse<TransactionStatsResponse>> getTransactionStats(
            @PathVariable UUID storeId,
            @RequestParam(name = "start_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success(transactionService.getTransactionStats(storeId, userId, startDate, endDate)));
    }
}
