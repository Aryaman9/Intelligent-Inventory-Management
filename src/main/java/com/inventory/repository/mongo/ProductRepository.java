package com.inventory.repository.mongo;

import com.inventory.document.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByTagsContaining(String tag);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByCategoryAndActiveTrue(String category);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);

    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryIgnoreCaseAndNameContainingIgnoreCase(
            String category, String name, Pageable pageable);

    Optional<Product> findByBarcodeAndActiveTrue(String barcode);
}
