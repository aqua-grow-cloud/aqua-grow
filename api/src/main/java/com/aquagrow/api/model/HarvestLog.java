package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@EntityListeners(com.aquagrow.api.security.TenantGuardListener.class)
@Table(name = "harvest_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class HarvestLog implements com.aquagrow.api.security.TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
    private Double commissionRate; // Commission rate as percentage (e.g., 0.2 for 0.2%)
    private Double commission;
    private Double revenue;
    private String buyer;
    private Boolean isFinal;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
