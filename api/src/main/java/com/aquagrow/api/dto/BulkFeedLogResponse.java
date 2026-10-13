package com.aquagrow.api.dto;

import java.util.List;

/** Result of a bulk feed log: created ids, total kg deducted and stock left. */
public record BulkFeedLogResponse(List<String> createdIds, double totalKg, double remainingStockKg) {
}
