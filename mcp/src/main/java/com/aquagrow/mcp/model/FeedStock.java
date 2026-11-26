package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Feed stock of one feed type as aqua-grow-api reports it (a sub-account sees its parent farm's stock). */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedStock {
    private String id;
    private String feedType;
    private Double currentStock;
    private Double threshold;
    private String unit;
    private String supplierId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime lastUpdated;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDate lastStockingDate;
    private String tenantId;

    public boolean isLowStock() {
        return currentStock != null && threshold != null && currentStock < threshold;
    }

    public boolean isCriticalStock() {
        return currentStock != null && threshold != null && currentStock < (threshold * 0.5);
    }
}
