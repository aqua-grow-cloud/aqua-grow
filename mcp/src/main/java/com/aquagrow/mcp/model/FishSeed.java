package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FishSeed {
    private String id;
    private String pondId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDate date;
    private String fishTypeId;
    private Integer quantity;
    private Double sizeInches;
    private Double sizeGrams;
    private Double unitPrice;
    private String vendor;
    private String tenantId;
    private String status;
}
