package com.aquagrow.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ponds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pond {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private Double area;
    private Double depth;
    private String type;

    @Column(nullable = false)
    private String status; // Active, Maintenance, Inactive

    private String currentBatchId;
}
