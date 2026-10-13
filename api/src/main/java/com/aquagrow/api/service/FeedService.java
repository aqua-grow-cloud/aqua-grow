package com.aquagrow.api.service;

import com.aquagrow.api.dto.BulkFeedLogRequest;
import com.aquagrow.api.dto.BulkFeedLogResponse;
import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final int MAX_BULK_ENTRIES = 366;
    private static final java.util.regex.Pattern TIME_PATTERN =
            java.util.regex.Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final FeedLogRepository feedLogRepository;
    private final FeedStockRepository feedStockRepository;
    private final EntityManager entityManager;

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

        feedLog.setTenantId(tenantId); // FeedLog belongs to the requesting tenant (sub-tenant or root)

        if (feedLog.getFeedType() != null && !feedLog.getFeedType().isEmpty()
                && feedLog.getAmount() != null && feedLog.getAmount() > 0) {
            deductFromInventory(TenantContext.getEffectiveFeedTenantId(),
                    feedLog.getFeedType(), feedLog.getAmount());
        }

        FeedLog saved = feedLogRepository.save(feedLog);

        log.info("Created feed log: {} (batch: {}, type: {}, amount: {} kg, tenant: {}, stock tenant: {})",
                saved.getId(), saved.getBatchId(), saved.getFeedType(), saved.getAmount(),
                tenantId, TenantContext.getEffectiveFeedTenantId());

        return saved;
    }

    /**
     * Creates several feed logs for one batch and feed type atomically.
     * Total stock is checked and deducted once, from the effective feed tenant
     * (the parent tenant for sub-tenants); if any step fails the whole
     * transaction rolls back, so no partial logs or partial deductions remain.
     *
     * @param request batch, feed type and the individual feedings (kg)
     * @return created ids, total kg deducted and the stock remaining
     * @throws BusinessException if the request is invalid, the feed type is unknown
     *                           or stock is insufficient
     */
    @Transactional
    public BulkFeedLogResponse createFeedLogsBulk(BulkFeedLogRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        validateBulkRequest(request);

        BigDecimal total = BigDecimal.ZERO;
        for (BulkFeedLogRequest.Entry entry : request.entries()) {
            total = total.add(BigDecimal.valueOf(entry.amount()));
        }
        double totalKg = total.doubleValue();

        String stockTenantId = TenantContext.getEffectiveFeedTenantId();
        deductFromInventory(stockTenantId, request.feedType(), totalKg);

        List<FeedLog> logs = new ArrayList<>();
        for (BulkFeedLogRequest.Entry entry : request.entries()) {
            logs.add(FeedLog.builder()
                    .batchId(request.batchId())
                    .feedType(request.feedType())
                    .date(entry.date())
                    .time(entry.time())
                    .amount(entry.amount())
                    .tenantId(tenantId)
                    .build());
        }
        List<String> ids = feedLogRepository.saveAll(logs).stream().map(FeedLog::getId).toList();

        log.info("Created {} feed logs (batch: {}, type: {}, total: {} kg, tenant: {}, stock tenant: {})",
                ids.size(), request.batchId(), request.feedType(), totalKg, tenantId, stockTenantId);

        return new BulkFeedLogResponse(ids, totalKg, remainingStock(stockTenantId, request.feedType()));
    }

    private void validateBulkRequest(BulkFeedLogRequest request) {
        if (request == null || request.batchId() == null || request.batchId().isBlank()) {
            throw new BusinessException("batchId is required");
        }
        if (request.feedType() == null || request.feedType().isBlank()) {
            throw new BusinessException("feedType is required");
        }
        List<BulkFeedLogRequest.Entry> entries = request.entries();
        if (entries == null || entries.isEmpty()) {
            throw new BusinessException("At least one feeding entry is required");
        }
        if (entries.size() > MAX_BULK_ENTRIES) {
            throw new BusinessException("Too many entries: at most " + MAX_BULK_ENTRIES + " per request");
        }
        HashSet<String> seen = new HashSet<>();
        for (BulkFeedLogRequest.Entry entry : entries) {
            if (entry == null || entry.date() == null) {
                throw new BusinessException("Every entry needs a date");
            }
            if (entry.amount() == null || !(entry.amount() > 0) || Double.isInfinite(entry.amount())) {
                throw new BusinessException("Every entry needs an amount greater than 0 (date: " + entry.date() + ")");
            }
            String time = entry.time();
            if (time != null && !time.isBlank() && !TIME_PATTERN.matcher(time).matches()) {
                throw new BusinessException("Invalid time '" + time + "' (expected HH:mm)");
            }
            if (!seen.add(entry.date() + "|" + (time == null ? "" : time))) {
                throw new BusinessException("Duplicate entry for " + entry.date()
                        + (time == null || time.isBlank() ? "" : " at " + time));
            }
        }
    }

    private double remainingStock(String stockTenantId, String feedType) {
        scopeTenantFilterTo(stockTenantId);
        try {
            return feedStockRepository.findByTenantIdAndFeedType(stockTenantId, feedType)
                    .map(FeedStock::getCurrentStock)
                    .orElse(0.0);
        } finally {
            scopeTenantFilterTo(TenantContext.getTenantId());
        }
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
        String stockTenantId = TenantContext.getEffectiveFeedTenantId();

        // Restore old amount back to inventory, then deduct new amount
        String oldFeedType = existing.getFeedType();
        Double oldAmount = existing.getAmount();
        String newFeedType = updated.getFeedType();
        Double newAmount = updated.getAmount();

        if (oldFeedType != null && !oldFeedType.isEmpty() && oldAmount != null && oldAmount > 0) {
            restoreToInventory(stockTenantId, oldFeedType, oldAmount);
        }
        if (newFeedType != null && !newFeedType.isEmpty()) {
            deductFromInventory(stockTenantId, newFeedType, newAmount);
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
        String stockTenantId = TenantContext.getEffectiveFeedTenantId();

        if (existing.getFeedType() != null && !existing.getFeedType().isEmpty()
                && existing.getAmount() != null && existing.getAmount() > 0) {
            restoreToInventory(stockTenantId, existing.getFeedType(), existing.getAmount());
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
        scopeTenantFilterTo(tenantId);
        try {
            feedStockRepository.findByTenantIdAndFeedType(tenantId, feedType)
                    .ifPresent(stock -> {
                        double current = stock.getCurrentStock() != null ? stock.getCurrentStock() : 0.0;
                        stock.setCurrentStock(current + amount);
                        feedStockRepository.save(stock);
                        log.info("Restored {} kg of {} to inventory (new total: {} kg, tenant: {})",
                                amount, feedType, stock.getCurrentStock(), tenantId);
                    });
        } finally {
            scopeTenantFilterTo(TenantContext.getTenantId());
        }
    }

    /**
     * Deducts feed amount from inventory stock.
     *
     * @param tenantId the tenant ID
     * @param feedType the feed type
     * @param amount the amount to deduct in kg
     * @throws BusinessException if insufficient stock
     */
    private void deductFromInventory(String stockTenantId, String feedType, Double amount) {
        if (amount == null || amount <= 0) return;
        scopeTenantFilterTo(stockTenantId);
        try {
            int updated = feedStockRepository.deductStock(
                    stockTenantId, feedType, amount, java.time.LocalDateTime.now());
            if (updated == 0) {
                feedStockRepository.findByTenantIdAndFeedType(stockTenantId, feedType)
                        .ifPresentOrElse(
                            stock -> {
                                throw new BusinessException(
                                    String.format("Insufficient global feed stock for '%s': available %.2f kg, requested %.2f kg",
                                        feedType, stock.getCurrentStock(), amount));
                            },
                            () -> {
                                throw new BusinessException(
                                    String.format("Feed type '%s' not found in global inventory. " +
                                        "Contact your network administrator.", feedType));
                            }
                        );
            }
            log.info("Deducted {} kg of {} from tenant {} stock", amount, feedType, stockTenantId);
        } finally {
            scopeTenantFilterTo(TenantContext.getTenantId());
        }
    }

    /**
     * Re-scopes the Hibernate {@code tenantFilter} to the given tenant id.
     * TenantAspect enables the filter pinned to the current (possibly sub-) tenant on every
     * request; global feed-stock operations must target the effective (root) feed tenant, so
     * we override the filter parameter around those operations. Both the deductStock bulk
     * UPDATE and the findByTenantIdAndFeedType SELECT are subject to this filter, so without
     * re-scoping a sub-tenant cannot see or modify the root's global inventory. Callers restore
     * the filter to the requesting tenant afterwards so FeedLog operations stay correctly scoped.
     */
    private void scopeTenantFilterTo(String tenantId) {
        if (tenantId == null) return;
        entityManager.unwrap(org.hibernate.Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
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

        List<FeedStock> lowStocks = feedStockRepository.findLowStockByTenantId(TenantContext.getEffectiveFeedTenantId());

        log.debug("Found {} low stock alerts for tenant: {}", lowStocks.size(), tenantId);

        return lowStocks;
    }
}
