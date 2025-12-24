package com.aquagrow.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_stock")
public class FeedStock {

    @Id
    private String id;

    @Column(name = "feed_type", nullable = false)
    private String feedType;

    @Column(name = "current_stock", nullable = false)
    private Double currentStock;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false)
    private String unit;

    @Column(name = "supplier_id")
    private String supplierId;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Constructors
    public FeedStock() {
        this.id = java.util.UUID.randomUUID().toString();
        this.lastUpdated = LocalDateTime.now();
        this.unit = "kg";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFeedType() {
        return feedType;
    }

    public void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    public Double getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Double currentStock) {
        this.currentStock = currentStock;
        this.lastUpdated = LocalDateTime.now();
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isLowStock() {
        return currentStock != null && threshold != null && currentStock < threshold;
    }

    public boolean isCriticalStock() {
        return currentStock != null && threshold != null && currentStock < (threshold * 0.5);
    }
}
