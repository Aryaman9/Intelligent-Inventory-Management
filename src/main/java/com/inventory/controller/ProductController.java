package com.inventory.controller;

import com.inventory.dto.request.CreateProductRequest;
import com.inventory.dto.request.UpdateProductRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.security.SecurityUtils;
import com.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        return ApiResponse.success("Product created successfully", productService.createProduct(request, userId));
    }

    @GetMapping
    public ApiResponse<PaginatedResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(productService.searchProducts(q, category, page, limit));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(productService.getCategories());
    }

    @GetMapping("/barcode/{barcode}")
    public ApiResponse<ProductResponse> getByBarcode(@PathVariable String barcode) {
        return ApiResponse.success(productService.getByBarcode(barcode));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id) {
        return ApiResponse.success(productService.getProductById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        return ApiResponse.success(productService.updateProduct(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable String id) {
        UUID userId = securityUtils.getCurrentUserId();
        productService.deleteProduct(id, userId);
        return ApiResponse.success("Product deleted", null);
    }
}
