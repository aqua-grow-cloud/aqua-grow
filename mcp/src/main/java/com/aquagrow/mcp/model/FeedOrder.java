package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDateTime;

/** A feed order as aqua-grow-api reports it. Fields the API does not send stay empty. */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedOrder {
    private String id;
    private String feedType;
    private Double quantity;
    @com.fasterxml.jackson.annotation.JsonFormat(with = com.fasterxml.jackson.annotation.JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
    private OrderStatus status;
    private String supplierId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime orderDate;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime confirmationDate;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime receivedDate;
    @com.fasterxml.jackson.annotation.JsonFormat(with = com.fasterxml.jackson.annotation.JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
    private PaymentStatus paymentStatus;
    private String notes;
    private String tenantId;
}
