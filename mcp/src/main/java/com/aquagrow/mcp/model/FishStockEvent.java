package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FishStockEvent {
    private String id;
    private String pondId;
    private String batchId;
    private LocalDate date;
    private FishStockEventType type;
    private String fishTypeId;
    private Integer quantityDelta;
    private Double avgWeightGram;
    private String referenceId;
    private String tenantId;
}
