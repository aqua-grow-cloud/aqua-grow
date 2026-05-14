package com.aquagrow.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * One row in the date×species growth analysis pivot table.
 * Each row represents a single sampling date.
 * Species columns are keyed by fishTypeId → ABW in grams.
 *
 * Returned by GET /api/sample-logs/pivot/by-date
 */
@Data
@Builder
public class DateSpeciesPivotRow {

    /** The sampling date */
    private LocalDate date;

    /**
     * fishTypeId → weighted average body weight (grams) for that species on this date.
     * Weighted avg = Σ(totalSampleWeight or avgWeight×count) / Σ(sampleCount).
     * Missing entry means no sample was recorded for that species on this date.
     */
    private Map<String, Double> abwBySpecies;

    /**
     * fishTypeId → total fish sampled for that species on this date.
     */
    private Map<String, Integer> countBySpecies;
}
