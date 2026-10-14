package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@EntityListeners(com.aquagrow.api.security.TenantGuardListener.class)
@Table(name = "feed_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FeedLog implements com.aquagrow.api.security.TenantOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String batchId;

    @Column(nullable = false)
    private LocalDate date;

    private String feedType;
    private Double amount; // in kg
    private String time; // HH:mm

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}
