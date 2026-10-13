package com.aquagrow.mcp.model;

import lombok.*;
import java.time.LocalDate;

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SalaryPayment {
    private String id;
    private String employeeId;
    private LocalDate date;
    private Double baseSalary;
    private Double advanceDeduction;
    private Double bonus;
    private Double netAmount;
    private String month;
    private String tenantId;
}
