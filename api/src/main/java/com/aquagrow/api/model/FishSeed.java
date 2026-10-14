package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@EntityListeners(com.aquagrow.api.security.TenantGuardListener.class)
@Table(name = "fish_seeds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishSeed implements com.aquagrow.api.security.TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String pondId;

    private LocalDate date;
    private String fishTypeId;
    private Integer quantity;
    private Double sizeInches;
    private Double sizeGrams;
    private Double unitPrice;
    private String vendor;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Builder.Default
    private String status = "Active"; // Active, Harvested
}
