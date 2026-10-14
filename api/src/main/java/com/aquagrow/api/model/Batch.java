package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(com.aquagrow.api.security.TenantGuardListener.class)
@Table(name = "batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Batch implements com.aquagrow.api.security.TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String tenantId; // CRITICAL: For multi-tenancy isolation
    private LocalDate stockingDate;
    private LocalDate estimatedHarvestDate;

    // Status is now derived from FishSeeds
    @Transient
    public String getStatus() {
        if (fishSeeds == null || fishSeeds.isEmpty()) {
            return "Unknown";
        }

        boolean allHarvested = fishSeeds.stream()
                .allMatch(seed -> "Harvested".equalsIgnoreCase(seed.getStatus())
                        || "Completed".equalsIgnoreCase(seed.getStatus()));

        return allHarvested ? "Completed" : "Active";
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "batch_id")
    @Builder.Default
    private List<FishSeed> fishSeeds = new ArrayList<>();
}
