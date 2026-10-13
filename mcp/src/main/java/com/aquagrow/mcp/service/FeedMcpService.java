package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.FeedStock;
import com.aquagrow.mcp.model.OrderStatus;
import com.aquagrow.mcp.repository.FeedLogRepository;
import com.aquagrow.mcp.repository.FeedOrderRepository;
import com.aquagrow.mcp.repository.FeedStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP Tools for Feed management (logs, stock, orders).
 * Tenant scoping is derived from the authenticated caller's JWT
 * (see TenantFilter/TenantContext) — never from an LLM-supplied parameter.
 */
@Service
@RequiredArgsConstructor
public class FeedMcpService {

    private final FeedLogRepository feedLogRepository;
    private final FeedOrderRepository feedOrderRepository;
    private final FeedStockRepository feedStockRepository;
    private final TenantFilterActivator tenantFilterActivator;

    // ---- Feed Logs ----

    @Tool(name = "list_feed_logs",
          description = "List all feed logs for the current tenant. Feed logs track daily feeding amounts per batch.")
    @Transactional(readOnly = true)
    public List<FeedLog> listFeedLogs() {
        String tenantId = tenantFilterActivator.activate();
        return feedLogRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_feed_logs_by_batch",
          description = "List feed logs for a specific batch.")
    @Transactional(readOnly = true)
    public List<FeedLog> listFeedLogsByBatch(String batchId) {
        String tenantId = tenantFilterActivator.activate();
        return feedLogRepository.findByTenantIdAndBatchId(tenantId, batchId);
    }

    @Tool(name = "create_feed_log",
          description = "Log a feeding event. Provide batchId, date (YYYY-MM-DD), feedType, amount (kg), and optional time (HH:mm).")
    @Transactional
    public FeedLog createFeedLog(String batchId, String date,
                                  String feedType, Double amount, String time) {
        String tenantId = tenantFilterActivator.activate();
        FeedLog log = FeedLog.builder()
            .tenantId(tenantId)
            .batchId(batchId)
            .date(LocalDate.parse(date))
            .feedType(feedType)
            .amount(amount)
            .time(time)
            .build();
        return feedLogRepository.save(log);
    }

    // ---- Feed Stock ----

    @Tool(name = "list_feed_stock",
          description = "List current feed stock levels for the current tenant. Includes low-stock and critical-stock indicators.")
    @Transactional(readOnly = true)
    public List<FeedStock> listFeedStock() {
        String tenantId = tenantFilterActivator.activate();
        return feedStockRepository.findByTenantId(tenantId);
    }

    @Tool(name = "get_low_stock_alerts",
          description = "Get all feed stocks that are below threshold for the current tenant.")
    @Transactional(readOnly = true)
    public List<FeedStock> getLowStockAlerts() {
        String tenantId = tenantFilterActivator.activate();
        return feedStockRepository.findByTenantId(tenantId).stream()
            .filter(FeedStock::isLowStock)
            .toList();
    }

    @Tool(name = "update_feed_stock",
          description = "Update the current stock quantity for a feed type (by stock ID).")
    @Transactional
    public FeedStock updateFeedStock(String stockId, Double newQuantity) {
        String tenantId = tenantFilterActivator.activate();
        FeedStock stock = feedStockRepository.findById(stockId)
            .filter(s -> tenantId.equals(s.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("FeedStock not found: " + stockId));
        stock.setCurrentStock(newQuantity);
        return feedStockRepository.save(stock);
    }

    // ---- Feed Orders ----

    @Tool(name = "list_feed_orders",
          description = "List all feed orders for the current tenant.")
    @Transactional(readOnly = true)
    public List<FeedOrder> listFeedOrders() {
        String tenantId = tenantFilterActivator.activate();
        return feedOrderRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_pending_feed_orders",
          description = "List all pending feed orders for the current tenant.")
    @Transactional(readOnly = true)
    public List<FeedOrder> listPendingFeedOrders() {
        String tenantId = tenantFilterActivator.activate();
        return feedOrderRepository.findByTenantIdAndStatus(tenantId, OrderStatus.PENDING);
    }

    @Tool(name = "create_feed_order",
          description = "Create a new feed order. Provide supplierId, feedType, and quantity (kg).")
    @Transactional
    public FeedOrder createFeedOrder(String supplierId,
                                      String feedType, Double quantity, String notes) {
        String tenantId = tenantFilterActivator.activate();
        FeedOrder order = FeedOrder.builder()
            .tenantId(tenantId)
            .supplierId(supplierId)
            .feedType(feedType)
            .quantity(quantity)
            .notes(notes)
            .build();
        return feedOrderRepository.save(order);
    }

    @Tool(name = "update_feed_order_status",
          description = "Update the status of a feed order. Valid statuses: PENDING, CONFIRMED, RECEIVED, CANCELLED.")
    @Transactional
    public FeedOrder updateFeedOrderStatus(String orderId, String status) {
        String tenantId = tenantFilterActivator.activate();
        FeedOrder order = feedOrderRepository.findById(orderId)
            .filter(o -> tenantId.equals(o.getTenantId()))
            .orElseThrow(() -> new IllegalArgumentException("FeedOrder not found: " + orderId));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        return feedOrderRepository.save(order);
    }
}
