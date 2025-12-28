package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "feed_orders")
@Data
@AllArgsConstructor
@Builder
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FeedOrder {

    @Id
    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();

    @Column(name = "feed_type", nullable = false)
    private String feedType;

    @Column(nullable = false)
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "supplier_id", nullable = false)
    private String supplierId;

    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "confirmation_date")
    private LocalDateTime confirmationDate;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(length = 1000)
    private String notes;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    public FeedOrder() {
        this.id = java.util.UUID.randomUUID().toString();
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }
}
