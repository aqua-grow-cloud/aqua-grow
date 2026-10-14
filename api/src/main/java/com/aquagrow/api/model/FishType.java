package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(com.aquagrow.api.security.TenantGuardListener.class)
@Table(name = "fish_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishType implements com.aquagrow.api.security.TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cultivation_type", length = 20)
    private CultivationType cultivationType;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
