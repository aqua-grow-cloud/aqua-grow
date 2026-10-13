package com.aquagrow.mcp.client;

/**
 * Feed stock as the API reports it to the caller. For sub-tenants this is the
 * parent tenant's global stock, which direct JPA reads would not return.
 */
public record FeedStockView(String id, String feedType, Double currentStock, Double threshold, String unit) {
}
