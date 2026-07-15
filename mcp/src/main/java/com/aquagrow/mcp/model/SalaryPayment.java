package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "salary_payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SalaryPayment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String employeeId;
    private LocalDate date;
    private Double baseSalary;
    private Double advanceDeduction;
    private Double bonus;
    private Double netAmount;
    @Column(name = "\"month\"") private String month;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
