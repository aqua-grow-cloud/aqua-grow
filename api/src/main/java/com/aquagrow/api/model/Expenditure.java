package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "expenditures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = String.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Expenditure {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String batchId;
    private LocalDate date;
    private String category; // Feed, Seed, Salary, Maintenance, Lease, NoReturn, etc.
    private String macroInvestment; // ADVANCE, LEASE, INVESTMENT, NO_RETURN
    private String subType;
    private Double amount;
    private String description;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
