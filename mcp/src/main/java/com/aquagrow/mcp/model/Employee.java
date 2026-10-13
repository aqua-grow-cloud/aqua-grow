package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    private String role;
    private Double salary;
    private Double advanceTaken;
    private String status;
    private LocalDate joinedDate;
    private String whatsappNumber;
}
