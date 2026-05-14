package com.aquagrow.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Pre-aggregated pivot row for a single batch, returned by GET /api/sample-logs/pivot.
 * Computed server-side so the frontend never needs to fetch raw log pages just to build a summary.
 */
@Data
@Builder
public class SamplePivotRow {

    /** Batch identifier */
    private String batchId;

    /** Total number of individual fish sampled (sum of sampleCount) */
    private int totalFishSampled;

    /**
     * Weighted average body weight in grams:
     * Σ(totalSampleWeight) / Σ(sampleCount)
     * Falls back to averageWeight × sampleCount when totalSampleWeight is absent.
     */
    private double weightedAvgWeight;

    /** Minimum per-record averageWeight across all sample logs for this batch */
    private double minWeight;

    /** Maximum per-record averageWeight across all sample logs for this batch */
    private double maxWeight;

    /**
     * Growth rate in grams per day:
     * (lastABW - firstABW) / days between first and last sample.
     * Null when fewer than 2 samples or insufficient weight data.
     */
    private Double growthRate;

    /** Date of the first (oldest) sample log */
    private LocalDate firstSampleDate;

    /** Date of the most recent sample log */
    private LocalDate lastSampleDate;

    /** Chronological per-sample entries used for sparkline / expanded rows */
    private List<SampleEntry> entries;

    @Data
    @Builder
    public static class SampleEntry {
        private LocalDate date;
        private int sampleCount;
        private double averageWeight;
        /** Days since the first sample for this batch (0-indexed) */
        private long dayNumber;
    }
}
