package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service for managing feed logs and feed consumption analytics.
 * Handles feed logging, inventory management, and FCR calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final FeedLogRepository feedLogRepository;
    private final FeedStockRepository feedStockRepository;

    /**
     * Creates a new feed log entry and updates feed stock inventory.
     * Sets the tenant ID from the current security context.
     *
     * @param feedLog the feed log to create
     * @return the created feed log with generated ID
     * @throws BusinessException if tenant context is missing or insufficient stock
     */
    @Transactional
    public FeedLog createFeedLog(FeedLog feedLog) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        feedLog.setTenantId(tenantId);

        // Deduct from inventory if feed type is specified
        if (feedLog.getFeedType() != null && !feedLog.getFeedType().isEmpty()) {
            deductFromInventory(tenantId, feedLog.getFeedType(), feedLog.getAmount());
        }

        FeedLog saved = feedLogRepository.save(feedLog);

        log.info("Created feed log: {} (batch: {}, type: {}, amount: {} kg, tenant: {})",
                saved.getId(), saved.getBatchId(), saved.getFeedType(), saved.getAmount(), tenantId);

        return saved;
    }

    /**
     * Retrieves all feed logs.
     *
     * @return list of all feed logs
     */
    @Transactional(readOnly = true)
    public List<FeedLog> getAllFeedLogs() {
        List<FeedLog> feedLogs = feedLogRepository.findAll();

        log.debug("Retrieved {} feed logs", feedLogs.size());

        return feedLogs;
    }

    /**
     * Retrieves all feed logs for a specific batch.
     *
     * @param batchId the batch ID
     * @return list of feed logs for the batch
     */
    @Transactional(readOnly = true)
    public List<FeedLog> getFeedLogsByBatch(String batchId) {
        List<FeedLog> feedLogs = feedLogRepository.findByBatchId(batchId);

        log.debug("Found {} feed logs for batch: {}", feedLogs.size(), batchId);

        return feedLogs;
    }

    /**
     * Calculates the total feed consumed for a batch.
     *
     * @param batchId the batch ID
     * @return total feed consumed in kg
     */
    @Transactional(readOnly = true)
    public Double calculateTotalFeedConsumed(String batchId) {
        List<FeedLog> feedLogs = feedLogRepository.findByBatchId(batchId);

        double totalFeed = feedLogs.stream()
                .mapToDouble(log -> log.getAmount() != null ? log.getAmount() : 0.0)
                .sum();

        log.debug("Calculated total feed consumed for batch {}: {} kg", batchId, totalFeed);

        return totalFeed;
    }

    /**
     * Calculates the total feed consumed for a batch within a date range.
     *
     * @param batchId the batch ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return total feed consumed in kg
     */
    @Transactional(readOnly = true)
    public Double calculateFeedConsumedInDateRange(String batchId, LocalDate startDate, LocalDate endDate) {
        List<FeedLog> feedLogs = feedLogRepository.findByBatchId(batchId);

        double totalFeed = feedLogs.stream()
                .filter(log -> !log.getDate().isBefore(startDate) && !log.getDate().isAfter(endDate))
                .mapToDouble(log -> log.getAmount() != null ? log.getAmount() : 0.0)
                .sum();

        log.debug("Calculated feed consumed for batch {} from {} to {}: {} kg",
                batchId, startDate, endDate, totalFeed);

        return totalFeed;
    }

    /**
     * Calculates feed consumption breakdown by feed type for a batch.
     *
     * @param batchId the batch ID
     * @return map of feed type to total amount consumed in kg
     */
    @Transactional(readOnly = true)
    public Map<String, Double> calculateFeedBreakdownByType(String batchId) {
        List<FeedLog> feedLogs = feedLogRepository.findByBatchId(batchId);

        Map<String, Double> breakdown = feedLogs.stream()
                .filter(log -> log.getFeedType() != null && log.getAmount() != null)
                .collect(Collectors.groupingBy(
                        FeedLog::getFeedType,
                        Collectors.summingDouble(FeedLog::getAmount)
                ));

        log.debug("Calculated feed breakdown for batch {}: {}", batchId, breakdown);

        return breakdown;
    }

    /**
     * Calculates the average daily feed consumption for a batch.
     *
     * @param batchId the batch ID
     * @return average daily feed in kg, or 0 if no feed logs
     */
    @Transactional(readOnly = true)
    public Double calculateAverageDailyFeed(String batchId) {
        List<FeedLog> feedLogs = feedLogRepository.findByBatchId(batchId);

        if (feedLogs.isEmpty()) {
            return 0.0;
        }

        // Group by date and sum amounts
        Map<LocalDate, Double> dailyFeed = feedLogs.stream()
                .collect(Collectors.groupingBy(
                        FeedLog::getDate,
                        Collectors.summingDouble(log -> log.getAmount() != null ? log.getAmount() : 0.0)
                ));

        double totalFeed = dailyFeed.values().stream().mapToDouble(Double::doubleValue).sum();
        int daysWithFeeding = dailyFeed.size();

        double averageDailyFeed = daysWithFeeding > 0 ? totalFeed / daysWithFeeding : 0.0;

        log.debug("Calculated average daily feed for batch {}: {} kg ({} days)",
                batchId, averageDailyFeed, daysWithFeeding);

        return averageDailyFeed;
    }

    /**
     * Calculates the Feed Conversion Ratio (FCR) for a batch.
     * FCR = Total Feed (kg) / Total Biomass Gain (kg)
     *
     * @param batchId the batch ID
     * @param totalFeedKg total feed consumed in kg
     * @param initialBiomassKg initial biomass in kg
     * @param currentBiomassKg current biomass in kg
     * @return FCR value, or null if biomass gain is zero or negative
     */
    @Transactional(readOnly = true)
    public Double calculateFCR(String batchId, Double totalFeedKg, Double initialBiomassKg, Double currentBiomassKg) {
        if (totalFeedKg == null || initialBiomassKg == null || currentBiomassKg == null) {
            log.warn("Cannot calculate FCR for batch {}: missing parameters", batchId);
            return null;
        }

        double biomassGain = currentBiomassKg - initialBiomassKg;

        if (biomassGain <= 0) {
            log.warn("Cannot calculate FCR for batch {}: biomass gain is {} kg", batchId, biomassGain);
            return null;
        }

        double fcr = totalFeedKg / biomassGain;

        log.debug("Calculated FCR for batch {}: {} (feed: {} kg, gain: {} kg)",
                batchId, fcr, totalFeedKg, biomassGain);

        return fcr;
    }

    /**
     * Updates an existing feed log, adjusting inventory for the difference in amount.
     *
     * @param id the feed log ID
     * @param updated the updated feed log data
     * @return the saved feed log
     * @throws BusinessException if the feed log is not found or insufficient stock
     */
    @Transactional
    public FeedLog updateFeedLog(String id, FeedLog updated) {
        FeedLog existing = feedLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Feed log not found: " + id));

        String tenantId = existing.getTenantId();

        // Restore old amount back to inventory, then deduct new amount
        String oldFeedType = existing.getFeedType();
        Double oldAmount = existing.getAmount();
        String newFeedType = updated.getFeedType();
        Double newAmount = updated.getAmount();

        if (oldFeedType != null && !oldFeedType.isEmpty() && oldAmount != null && oldAmount > 0) {
            restoreToInventory(tenantId, oldFeedType, oldAmount);
        }
        if (newFeedType != null && !newFeedType.isEmpty()) {
            deductFromInventory(tenantId, newFeedType, newAmount);
        }

        existing.setBatchId(updated.getBatchId() != null ? updated.getBatchId() : existing.getBatchId());
        existing.setDate(updated.getDate() != null ? updated.getDate() : existing.getDate());
        existing.setFeedType(newFeedType);
        existing.setAmount(newAmount);
        existing.setTime(updated.getTime() != null ? updated.getTime() : existing.getTime());

        FeedLog saved = feedLogRepository.save(existing);
        log.info("Updated feed log: {} for tenant: {}", id, tenantId);
        return saved;
    }

    /**
     * Deletes a feed log and restores its amount back to feed stock inventory.
     *
     * @param id the feed log ID
     * @throws BusinessException if the feed log is not found
     */
    @Transactional
    public void deleteFeedLog(String id) {
        FeedLog existing = feedLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Feed log not found: " + id));

        String tenantId = existing.getTenantId();

        if (existing.getFeedType() != null && !existing.getFeedType().isEmpty()
                && existing.getAmount() != null && existing.getAmount() > 0) {
            restoreToInventory(tenantId, existing.getFeedType(), existing.getAmount());
        }

        feedLogRepository.delete(existing);
        log.info("Deleted feed log: {} for tenant: {}", id, tenantId);
    }

    /**
     * Restores feed amount back to inventory stock.
     *
     * @param tenantId the tenant ID
     * @param feedType the feed type
     * @param amount the amount to restore in kg
     */
    private void restoreToInventory(String tenantId, String feedType, Double amount) {
        if (amount == null || amount <= 0) return;
        feedStockRepository.findByTenantIdAndFeedType(tenantId, feedType)
                .ifPresent(stock -> {
                    double current = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;
                    stock.setCurrentStock(current + amount);
                    feedStockRepository.save(stock);
                    log.info("Restored {} kg of {} to inventory (new total: {} kg, tenant: {})",
                            amount, feedType, stock.getCurrentStock(), tenantId);
                });
    }

    /**
     * Deducts feed amount from inventory stock.
     *
     * @param tenantId the tenant ID
     * @param feedType the feed type
     * @param amount the amount to deduct in kg
     * @throws BusinessException if insufficient stock
     */
    private void deductFromInventory(String tenantId, String feedType, Double amount) {
        if (amount == null || amount <= 0) {
            return;
        }

        feedStockRepository.findByTenantIdAndFeedType(tenantId, feedType)
                .ifPresent(stock -> {
                    double currentStock = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;

                    if (currentStock < amount) {
                        log.warn("Insufficient feed stock for type {}: current {} kg, requested {} kg",
                                feedType, currentStock, amount);
                        throw new BusinessException(
                                String.format("Insufficient feed stock for %s: available %.2f kg, requested %.2f kg",
                                        feedType, currentStock, amount));
                    }

                    stock.setCurrentStock(currentStock - amount);
                    feedStockRepository.save(stock);

                    log.info("Deducted {} kg of {} from inventory (remaining: {} kg, tenant: {})",
                            amount, feedType, stock.getCurrentStock(), tenantId);

                    // Check if stock is low after deduction
                    if (stock.isLowStock()) {
                        log.warn("Low stock alert for {}: {} kg (threshold: {} kg)",
                                feedType, stock.getCurrentStock(), stock.getThreshold());
                    }
                    if (stock.isCriticalStock()) {
                        log.error("CRITICAL stock alert for {}: {} kg (threshold: {} kg)",
                                feedType, stock.getCurrentStock(), stock.getThreshold());
                    }
                });
    }

    /**
     * Retrieves all feed stock records with low stock levels.
     *
     * @return list of feed stocks below threshold
     */
    @Transactional(readOnly = true)
    public List<FeedStock> getLowStockAlerts() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        List<FeedStock> lowStocks = feedStockRepository.findLowStockByTenantId(tenantId);

        log.debug("Found {} low stock alerts for tenant: {}", lowStocks.size(), tenantId);

        return lowStocks;
    }
}
