package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "feed_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FeedLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false) private String batchId;
    @Column(nullable = false) private LocalDate date;
    private String feedType;
    private Double amount;
    private String time;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
}
