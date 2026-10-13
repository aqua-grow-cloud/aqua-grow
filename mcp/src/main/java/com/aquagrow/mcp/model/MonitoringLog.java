package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "monitoring_logs")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MonitoringLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String batchId;
    @Column(nullable = false) private LocalDate date;
    private Double doLevel;
    private Double ph;
    private Double temperature;
    private Double ammonia;
    private Double salinity;
    private Double turbidity;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
