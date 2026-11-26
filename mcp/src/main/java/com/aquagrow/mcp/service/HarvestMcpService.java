package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.FishStockEvent;
import com.aquagrow.mcp.model.HarvestLog;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Read-only harvest and fish-stock-event tools. Harvest revenue is stored net of commission. */
@Service
@RequiredArgsConstructor
public class HarvestMcpService {

    private final FarmDataClient farmData;
    private final ResolverService resolver;

    @Tool(name = "list_harvest_logs",
          description = "List all harvests with weight, rate, commission, net revenue and buyer.")
    public List<HarvestLog> listHarvestLogs() {
        return farmData.harvestLogs();
    }

    @Tool(name = "list_harvest_logs_by_batch",
          description = "List the harvests of one batch.")
    public List<HarvestLog> listHarvestLogsByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.harvestLogsByBatch(resolver.resolveAnyBatch(batchId).id());
    }

    @Tool(name = "list_fish_stock_events",
          description = "List all fish population events (SEED, HARVEST, MORTALITY, SAMPLING, TRANSFER).")
    public List<FishStockEvent> listFishStockEvents() {
        return farmData.fishStockEvents();
    }

    @Tool(name = "list_fish_stock_events_by_batch",
          description = "List the fish population events of one batch.")
    public List<FishStockEvent> listFishStockEventsByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.fishStockEventsByBatch(resolver.resolveAnyBatch(batchId).id());
    }

    @Tool(name = "get_total_revenue_by_batch",
          description = "Total harvest revenue of one batch, net of commission. For a full profit picture use get_batch_financials.")
    public BatchTotal getTotalRevenueByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        var batch = resolver.resolveAnyBatch(batchId);
        List<HarvestLog> logs = farmData.harvestLogsByBatch(batch.id());
        double total = logs.stream().mapToDouble(h -> h.getRevenue() == null ? 0.0 : h.getRevenue()).sum();
        long missing = logs.stream().filter(h -> h.getRevenue() == null).count();

        List<String> notes = new ArrayList<>();
        notes.add("Revenue is net of commission.");
        if (!"Completed".equals(batch.status())) {
            notes.add("The batch is not fully harvested (status " + batch.status() + "), so this total is partial.");
        }
        if (missing > 0) {
            notes.add(missing + " harvest log(s) have no revenue recorded and count as 0.");
        }
        return new BatchTotal(batch.id(), batch.name(), total, logs.size(), notes);
    }
}
