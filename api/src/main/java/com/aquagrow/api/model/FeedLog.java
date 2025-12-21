package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "feed_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String batchId;

    @Column(nullable = false)
    private LocalDate date;

    private String feedType;
    private Double amount; // in kg
    private String time; // HH:mm
}
