package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @Builder
public class FeedStock {
    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();
    private String feedType;
    private Double currentStock;
    private Double threshold;
    @Builder.Default private String unit = "kg";
    private String supplierId;
    @Builder.Default private LocalDateTime lastUpdated = LocalDateTime.now();
    private LocalDate lastStockingDate;
    private String tenantId;

    public FeedStock() {
        this.id = java.util.UUID.randomUUID().toString();
        this.lastUpdated = LocalDateTime.now();
        this.unit = "kg";
    }

    public boolean isLowStock() {
        return currentStock != null && threshold != null && currentStock < threshold;
    }

    public boolean isCriticalStock() {
        return currentStock != null && threshold != null && currentStock < (threshold * 0.5);
    }
}
