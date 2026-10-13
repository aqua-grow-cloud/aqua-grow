package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "fish_seeds")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishSeed {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String pondId;
    private LocalDate date;
    private String fishTypeId;
    private Integer quantity;
    private Double sizeInches;
    private Double sizeGrams;
    private Double unitPrice;
    private String vendor;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Builder.Default private String status = "Active";
}
