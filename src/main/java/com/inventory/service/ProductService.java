package com.inventory.service;

import com.inventory.document.Product;
import com.inventory.dto.request.CreateProductRequest;
import com.inventory.dto.request.UpdateProductRequest;
import com.inventory.dto.response.PaginatedResponse;
import com.inventory.dto.response.ProductResponse;
import com.inventory.exception.ConflictException;
import com.inventory.exception.ErrorCode;
import com.inventory.exception.ForbiddenException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.ProductMapper;
import com.inventory.repository.mongo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final MongoTemplate mongoTemplate;

    public ProductResponse createProduct(CreateProductRequest request, UUID userId) {
        Product product = productMapper.toDocument(request);
        product.setCreatedBy(userId.toString());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        try {
            return productMapper.toResponse(productRepository.save(product));
        } catch (DuplicateKeyException e) {
            throw new ConflictException("A product with this barcode already exists", ErrorCode.INV_002);
        }
    }

    public ProductResponse getProductById(String productId) {
        return productMapper.toResponse(findProduct(productId));
    }

    public PaginatedResponse<ProductResponse> searchProducts(String query, String category, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("name").ascending());
        Page<Product> productPage;

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasCategory = category != null && !category.isBlank();

        if (hasQuery && hasCategory) {
            productPage = productRepository.findByActiveTrueAndCategoryIgnoreCaseAndNameContainingIgnoreCase(
                    category, query, pageable);
        } else if (hasQuery) {
            productPage = productRepository.findByActiveTrueAndNameContainingIgnoreCase(query, pageable);
        } else if (hasCategory) {
            productPage = productRepository.findByActiveTrueAndCategoryIgnoreCase(category, pageable);
        } else {
            productPage = productRepository.findByActiveTrue(pageable);
        }

        Page<ProductResponse> responsePage = productPage.map(productMapper::toResponse);
        return PaginatedResponse.<ProductResponse>builder()
                .items(responsePage.getContent())
                .pagination(PaginatedResponse.PaginationInfo.builder()
                        .total(responsePage.getTotalElements())
                        .page(page)
                        .limit(limit)
                        .pages(responsePage.getTotalPages())
                        .build())
                .build();
    }

    public ProductResponse updateProduct(String productId, UpdateProductRequest request, UUID userId) {
        Product product = findProduct(productId);
        verifyCreator(product, userId);
        productMapper.updateDocument(request, product);
        product.setUpdatedAt(LocalDateTime.now());
        try {
            return productMapper.toResponse(productRepository.save(product));
        } catch (DuplicateKeyException e) {
            throw new ConflictException("A product with this barcode already exists", ErrorCode.INV_002);
        }
    }

    public void deleteProduct(String productId, UUID userId) {
        Product product = findProduct(productId);
        verifyCreator(product, userId);
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    public List<String> getCategories() {
        return mongoTemplate.findDistinct(
                Query.query(Criteria.where("active").is(true)),
                "category",
                Product.class,
                String.class
        );
    }

    public ProductResponse getByBarcode(String barcode) {
        return productRepository.findByBarcodeAndActiveTrue(barcode)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with barcode: " + barcode));
    }

    private Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private void verifyCreator(Product product, UUID userId) {
        if (!userId.toString().equals(product.getCreatedBy())) {
            throw new ForbiddenException("You do not have permission to modify this product");
        }
    }
}
