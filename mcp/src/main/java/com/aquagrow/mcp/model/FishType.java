package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fish_types")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishType {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String name;
    private String scientificName;
    @Enumerated(EnumType.STRING) @Column(name = "cultivation_type", length = 20)
    private CultivationType cultivationType;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
