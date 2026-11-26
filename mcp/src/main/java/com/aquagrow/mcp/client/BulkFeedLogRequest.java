package com.aquagrow.mcp.client;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/** Mirrors aqua-grow-api's POST /api/feed-logs/bulk request. Amounts in kg. */
public record BulkFeedLogRequest(String batchId, String feedType, List<Entry> entries) {

    public record Entry(@JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate date, String time, Double amount) {
    }
}
