package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "expenditures")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Expenditure {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String batchId;
    private LocalDate date;
    private String category;
    private String macroInvestment;
    private String subType;
    private Double amount;
    private String description;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
