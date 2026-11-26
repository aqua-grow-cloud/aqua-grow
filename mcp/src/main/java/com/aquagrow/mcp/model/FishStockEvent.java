package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FishStockEvent {
    private String id;
    private String pondId;
    private String batchId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDate date;
    @com.fasterxml.jackson.annotation.JsonFormat(with = com.fasterxml.jackson.annotation.JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
    private FishStockEventType type;
    private String fishTypeId;
    private Integer quantityDelta;
    private Double avgWeightGram;
    private String referenceId;
    private String tenantId;
}
