package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "fish_stock_events")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FishStockEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String pondId;
    @Column(nullable = false) private String batchId;
    @Column(nullable = false) private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private FishStockEventType type;
    private String fishTypeId;
    @Column(nullable = false) private Integer quantityDelta;
    private Double avgWeightGram;
    private String referenceId;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
