package com.aquagrow.mcp.model;

import lombok.*;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FishType {
    private String id;
    private String name;
    private String scientificName;
    @com.fasterxml.jackson.annotation.JsonFormat(with = com.fasterxml.jackson.annotation.JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
    private CultivationType cultivationType;
    private String tenantId;
}
