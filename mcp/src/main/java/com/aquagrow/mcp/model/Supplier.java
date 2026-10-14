package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/** A supplier as aqua-grow-api reports it. Fields the API does not send stay empty. */
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier {
    private String id;
    private String name;
    private String phone;
    private String whatsappNumber;
    private String email;
    private String address;
    private String externalId;
    private List<String> feedTypes;
    private String orderTemplate;
    private String tenantId;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime updatedAt;
}
