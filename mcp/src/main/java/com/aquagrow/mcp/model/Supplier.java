package com.aquagrow.mcp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Data @AllArgsConstructor @Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Supplier {
    @Id @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();
    @Column(nullable = false) private String name;
    private String phone;
    @Column(name = "whatsapp_number") private String whatsappNumber;
    private String email;
    private String address;
    @Column(name = "external_id") private String externalId;
    @ElementCollection
    @CollectionTable(name = "supplier_feed_types", joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "feed_type")
    private List<String> feedTypes;
    @Column(name = "order_template", length = 2000) private String orderTemplate;
    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "created_at") @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();

    public Supplier() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
