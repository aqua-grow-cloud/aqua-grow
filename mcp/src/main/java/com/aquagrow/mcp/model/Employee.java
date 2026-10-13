package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {
    private String id;
    private String name;
    private String tenantId;
    private String role;
    private Double salary;
    private Double advanceTaken;
    private String status;
    private LocalDate joinedDate;
    private String whatsappNumber;
}
