package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HarvestLog {
    private String id;
    private String batchId;
    private String pondId;
    private String fishTypeId;
    private LocalDate date;
    private Double biomassWeight;
    private Integer count;
    private Double avgWeightGrams;
    private Double basicRate;
    private Double finalRate;
    private Double discountWeight;
    private Double actualWeight;
    private Double commissionRate;
    private Double commission;
    private Double revenue;
    private String buyer;
    private Boolean isFinal;
    private String tenantId;
}
