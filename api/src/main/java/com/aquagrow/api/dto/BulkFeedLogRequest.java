package com.aquagrow.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to log several feedings of one feed type against one batch in a
 * single all-or-nothing operation. Amounts are in kg.
 */
public record BulkFeedLogRequest(String batchId, String feedType, List<Entry> entries) {

    /** One feeding: a date, an optional HH:mm time, and an amount in kg. */
    public record Entry(LocalDate date, String time, Double amount) {
    }
}
