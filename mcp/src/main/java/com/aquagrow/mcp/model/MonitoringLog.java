package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MonitoringLog {
    private String id;
    private String batchId;
    private LocalDate date;
    private Double doLevel;
    private Double ph;
    private Double temperature;
    private Double ammonia;
    private Double salinity;
    private Double turbidity;
    private String tenantId;
}
