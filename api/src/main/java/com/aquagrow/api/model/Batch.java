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
@Table(name = "batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String pondId;

    private String name;
    private LocalDate stockingDate;
    private LocalDate estimatedHarvestDate;
    private String species;
    private Integer initialQuantity;
    private Double seedSize;
    private String status; // Active, Harvested

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "batch_id")
    @Builder.Default
    private List<FishSeed> fishSeeds = new ArrayList<>();
}
