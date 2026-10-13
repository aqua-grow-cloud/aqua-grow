package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ponds")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Pond {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String name;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    private Double area;
    private Double depth;
    private String type;
    @Column(nullable = false) private String status;
    private String currentBatchId;
    private Double latitude;
    private Double longitude;
}
