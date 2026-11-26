package com.aquagrow.mcp.client;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** The saved sample log as returned by aqua-grow-api (unknown fields are ignored). */
public record SampleLogResult(
        String id,
        String batchId,
        String fishTypeId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date,
        Integer sampleCount,
        Double totalSampleWeight,
        Double averageWeight,
        Double averageLength,
        Double survivalEstimate) {
}
