package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Expenditure {
    private String id;
    private String batchId;
    private LocalDate date;
    private String category;
    private String macroInvestment;
    private String subType;
    private Double amount;
    private String description;
    private String tenantId;
}
