package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "fish_seeds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishSeed {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String pondId;

    private LocalDate date;
    private String type;
    private Integer quantity;
    private Double sizeInches;
    private Double sizeGrams;
    private Double unitPrice;
    private String vendor;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
