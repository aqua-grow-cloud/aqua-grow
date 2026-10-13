package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.FeedStock;
import com.aquagrow.mcp.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only feed tools. Feed is logged through preview_feeding / confirm_feeding, which keep the stock in step.
 * Stock is what the API reports, so a sub-account sees the parent farm's shared stock.
 */
@Service
@RequiredArgsConstructor
public class FeedMcpService {

    private final FarmDataClient farmData;
    private final ResolverService resolver;

    @Tool(name = "list_feed_logs",
          description = "List all feed logs (daily feeding records: date, feed type, amount in kg).")
    public List<FeedLog> listFeedLogs() {
        return farmData.feedLogs();
    }

    @Tool(name = "list_feed_logs_by_batch",
          description = "List the feed logs of one batch.")
    public List<FeedLog> listFeedLogsByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.feedLogsByBatch(resolver.resolveAnyBatch(batchId).id());
    }

    @Tool(name = "list_feed_stock",
          description = "List current feed stock levels per feed type, with reorder thresholds and low-stock indicators. "
                  + "Sub-accounts see the parent farm's shared stock.")
    public List<FeedStock> listFeedStock() {
        return farmData.feedStock();
    }

    @Tool(name = "get_low_stock_alerts",
          description = "List the feed types whose stock is below their reorder threshold.")
    public List<FeedStock> getLowStockAlerts() {
        return farmData.lowStock();
    }

    @Tool(name = "list_feed_orders",
          description = "List all feed orders (quantities and status; orders carry no price).")
    public List<FeedOrder> listFeedOrders() {
        return farmData.feedOrders();
    }

    @Tool(name = "list_pending_feed_orders",
          description = "List the feed orders that are still PENDING.")
    public List<FeedOrder> listPendingFeedOrders() {
        return farmData.feedOrders().stream().filter(o -> o.getStatus() == OrderStatus.PENDING).toList();
    }
}
