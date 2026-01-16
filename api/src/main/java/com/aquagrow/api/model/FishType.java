package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fish_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String scientificName;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
