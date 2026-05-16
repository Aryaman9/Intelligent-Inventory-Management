package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = {"store"})
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String unit = "piece";

    @Column(name = "low_stock_threshold", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lowStockThreshold = BigDecimal.TEN;

    @Column(name = "reorder_quantity", precision = 10, scale = 2)
    private BigDecimal reorderQuantity;

    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;

    @Column(name = "last_sold_at")
    private LocalDateTime lastSoldAt;

    @Column(length = 100)
    private String location;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Version
    private Long version;
}
