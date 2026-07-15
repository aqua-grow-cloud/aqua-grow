package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "sample_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SampleLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String batchId;
    private String fishTypeId;
    @Column(nullable = false) private LocalDate date;
    private Integer sampleCount;
    private Double totalSampleWeight;
    private Double averageWeight;
    private Double averageLength;
    private Double survivalEstimate;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
