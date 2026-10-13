package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Batch {
    private String id;
    private String name;
    private String tenantId;
    private LocalDate stockingDate;
    private LocalDate estimatedHarvestDate;

    @Builder.Default
    private List<FishSeed> fishSeeds = new ArrayList<>();

    public String getStatus() {
        if (fishSeeds == null || fishSeeds.isEmpty()) return "Unknown";
        boolean allHarvested = fishSeeds.stream()
            .allMatch(s -> "Harvested".equalsIgnoreCase(s.getStatus()) || "Completed".equalsIgnoreCase(s.getStatus()));
        return allHarvested ? "Completed" : "Active";
    }
}
