package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_stock")
@Data
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FeedStock {

    @Id
    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();

    @Column(name = "feed_type", nullable = false)
    private String feedType;

    @Column(name = "current_stock", nullable = false)
    private Double currentStock;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    @Builder.Default
    private String unit = "kg";

    @Column(name = "supplier_id")
    private String supplierId;

    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(name = "last_stocking_date")
    private LocalDate lastStockingDate;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public FeedStock() {
        this.id = java.util.UUID.randomUUID().toString();
        this.lastUpdated = LocalDateTime.now();
        this.unit = "kg";
    }

    public void setCurrentStock(Double currentStock) {
        this.currentStock = currentStock;
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isLowStock() {
        return currentStock != null && threshold != null && currentStock < threshold;
    }

    public boolean isCriticalStock() {
        return currentStock != null && threshold != null && currentStock < (threshold * 0.5);
    }
}
