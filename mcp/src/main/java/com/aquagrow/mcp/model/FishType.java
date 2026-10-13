package com.aquagrow.mcp.model;

import lombok.*;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FishType {
    private String id;
    private String name;
    private String scientificName;
    private CultivationType cultivationType;
    private String tenantId;
}
