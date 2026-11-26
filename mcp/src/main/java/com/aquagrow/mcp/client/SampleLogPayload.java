package com.aquagrow.mcp.client;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/** Body for aqua-grow-api's POST /api/sample-logs. The API sets the tenant. */
public record SampleLogPayload(
        String batchId,
        String fishTypeId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date,
        Integer sampleCount,
        Double totalSampleWeight,
        Double averageWeight,
        Double averageLength,
        Double survivalEstimate) {
}
