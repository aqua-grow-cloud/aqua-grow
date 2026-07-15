package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batches")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Batch {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    private LocalDate stockingDate;
    private LocalDate estimatedHarvestDate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "batch_id")
    @Builder.Default
    private List<FishSeed> fishSeeds = new ArrayList<>();

    @Transient
    public String getStatus() {
        if (fishSeeds == null || fishSeeds.isEmpty()) return "Unknown";
        boolean allHarvested = fishSeeds.stream()
            .allMatch(s -> "Harvested".equalsIgnoreCase(s.getStatus()) || "Completed".equalsIgnoreCase(s.getStatus()));
        return allHarvested ? "Completed" : "Active";
    }
}
