package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.HarvestLog;
import com.aquagrow.mcp.model.FishStockEvent;
import com.aquagrow.mcp.repository.HarvestLogRepository;
import com.aquagrow.mcp.repository.FishStockEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP Tools for Harvest management.
 * Tenant scoping is derived from the authenticated caller's JWT
 * (see TenantFilter/TenantContext) — never from an LLM-supplied parameter.
 */
@Service
@RequiredArgsConstructor
public class HarvestMcpService {

    private final HarvestLogRepository harvestLogRepository;
    private final FishStockEventRepository fishStockEventRepository;
    private final TenantFilterActivator tenantFilterActivator;

    @Tool(name = "list_harvest_logs",
          description = "List all harvest logs for the current tenant. Includes revenue, buyer, biomass weight, and commission details.")
    @Transactional(readOnly = true)
    public List<HarvestLog> listHarvestLogs() {
        String tenantId = tenantFilterActivator.activate();
        return harvestLogRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_harvest_logs_by_batch",
          description = "List harvest logs for a specific batch.")
    @Transactional(readOnly = true)
    public List<HarvestLog> listHarvestLogsByBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return harvestLogRepository.findByTenantIdAndBatchId(tenantId, batchId);
    }

    @Tool(name = "create_harvest_log",
          description = "Record a harvest event. Provide batchId, pondId, fishTypeId, date (YYYY-MM-DD), biomassWeight (kg), count, avgWeightGrams, basicRate, finalRate, buyer.")
    @Transactional
    public HarvestLog createHarvestLog(String batchId, String pondId,
                                        String fishTypeId, String date, Double biomassWeight,
                                        Integer count, Double avgWeightGrams, Double basicRate,
                                        Double finalRate, Double discountWeight, Double actualWeight,
                                        Double commissionRate, String buyer, Boolean isFinal) {
        String tenantId = tenantFilterActivator.activate();
        double commission = actualWeight != null && commissionRate != null
            ? actualWeight * commissionRate / 100 : 0;
        double revenue = actualWeight != null && finalRate != null
            ? actualWeight * finalRate - commission : 0;

        HarvestLog log = HarvestLog.builder()
            .tenantId(tenantId)
            .batchId(batchId)
            .pondId(pondId)
            .fishTypeId(fishTypeId)
            .date(LocalDate.parse(date))
            .biomassWeight(biomassWeight)
            .count(count)
            .avgWeightGrams(avgWeightGrams)
            .basicRate(basicRate)
            .finalRate(finalRate)
            .discountWeight(discountWeight)
            .actualWeight(actualWeight)
            .commissionRate(commissionRate)
            .commission(commission)
            .revenue(revenue)
            .buyer(buyer)
            .isFinal(isFinal)
            .build();
        return harvestLogRepository.save(log);
    }

    @Tool(name = "list_fish_stock_events",
          description = "List all fish stock events for the current tenant. Events include SEED, HARVEST, MORTALITY, SAMPLING, TRANSFER_OUT, TRANSFER_IN.")
    @Transactional(readOnly = true)
    public List<FishStockEvent> listFishStockEvents() {
        String tenantId = tenantFilterActivator.activate();
        return fishStockEventRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_fish_stock_events_by_batch",
          description = "List fish stock events for a specific batch.")
    @Transactional(readOnly = true)
    public List<FishStockEvent> listFishStockEventsByBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return fishStockEventRepository.findByTenantIdAndBatchId(tenantId, batchId);
    }

    @Tool(name = "get_total_revenue_by_batch",
          description = "Calculate the total harvest revenue for a specific batch.")
    @Transactional(readOnly = true)
    public double getTotalRevenueByBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return harvestLogRepository.findByTenantIdAndBatchId(tenantId, batchId)
            .stream()
            .mapToDouble(h -> h.getRevenue() != null ? h.getRevenue() : 0)
            .sum();
    }
}
