package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedLog {
    private String id;
    private String batchId;
    private LocalDate date;
    private String feedType;
    private Double amount;
    private String time;
    private String tenantId;
}
