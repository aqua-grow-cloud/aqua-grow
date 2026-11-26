package com.aquagrow.mcp.client;

import java.util.List;

/** Mirrors aqua-grow-api's bulk feed log response. */
public record BulkFeedLogResponse(List<String> createdIds, double totalKg, double remainingStockKg) {
}
