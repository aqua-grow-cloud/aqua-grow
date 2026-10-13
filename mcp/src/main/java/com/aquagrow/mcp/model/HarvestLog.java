package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "harvest_logs")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class HarvestLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String batchId;
    private String pondId;
    private String fishTypeId;
    private LocalDate date;
    private Double biomassWeight;
    private Integer count;
    private Double avgWeightGrams;
    private Double basicRate;
    private Double finalRate;
    private Double discountWeight;
    private Double actualWeight;
    private Double commissionRate;
    private Double commission;
    private Double revenue;
    private String buyer;
    private Boolean isFinal;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
