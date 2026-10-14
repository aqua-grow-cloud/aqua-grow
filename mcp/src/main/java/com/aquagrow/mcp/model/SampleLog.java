package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SampleLog {
    private String id;
    private String batchId;
    private String fishTypeId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDate date;
    private Integer sampleCount;
    private Double totalSampleWeight;
    private Double averageWeight;
    private Double averageLength;
    private Double survivalEstimate;
    private String tenantId;
}
