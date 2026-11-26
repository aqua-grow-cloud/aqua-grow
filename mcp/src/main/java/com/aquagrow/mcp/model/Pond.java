package com.aquagrow.mcp.model;

import lombok.*;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Pond {
    private String id;
    private String name;
    private String tenantId;
    private Double area;
    private Double depth;
    private String type;
    private String status;
    private String currentBatchId;
    private Double latitude;
    private Double longitude;
}
