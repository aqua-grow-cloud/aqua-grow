package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "harvest_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HarvestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String batchId;
    private String pondId;
    private String fishType;
    private LocalDate date;
    private Double biomassWeight;
    private Integer count;
    private Double avgWeightGrams;
    private Double basicRate;
    private Double finalRate;
    private Double discountWeight;
    private Double actualWeight;
    private Double commission;
    private Double revenue;
    private String buyer;
    private Boolean isFinal;
}
