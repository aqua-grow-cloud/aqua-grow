package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @Builder
public class Supplier {
    @Builder.Default
    private String id = java.util.UUID.randomUUID().toString();
    private String name;
    private String phone;
    private String whatsappNumber;
    private String email;
    private String address;
    private String externalId;
    private List<String> feedTypes;
    private String orderTemplate;
    private String tenantId;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default private LocalDateTime updatedAt = LocalDateTime.now();

    public Supplier() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

}
