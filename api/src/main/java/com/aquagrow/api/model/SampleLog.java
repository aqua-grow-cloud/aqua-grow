package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "sample_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String batchId;

    @Column(nullable = false)
    private LocalDate date;

    private Integer sampleCount;
    private Double totalSampleWeight;
    private Double averageWeight;
    private Double averageLength;
    private Double survivalEstimate;

    @Embedded
    private WaterQuality waterQuality;
}
