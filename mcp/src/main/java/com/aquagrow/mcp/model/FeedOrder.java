package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDateTime;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @Builder
public class FeedOrder {
    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();
    private String feedType;
    private Double quantity;
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    private String supplierId;
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();
    private LocalDateTime confirmationDate;
    private LocalDateTime receivedDate;
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private String notes;
    private String tenantId;

    public FeedOrder() {
        this.id = java.util.UUID.randomUUID().toString();
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
        this.paymentStatus = PaymentStatus.UNPAID;
    }
}
