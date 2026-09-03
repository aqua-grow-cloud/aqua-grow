package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for Harvest operations.
 * Encapsulates business logic for harvest logging, stock events, and batch closure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HarvestService {

    private final HarvestLogRepository harvestLogRepository;
    private final BatchRepository batchRepository;
    private final PondRepository pondRepository;
    private final FishStockEventRepository fishStockEventRepository;
    private final FishTypeRepository fishTypeRepository;

    /**
     * Get all harvest logs with optional filtering.
     */
    public List<HarvestLog> getAllHarvestLogs(String batchId, String fishTypeId, String pondId) {
        HarvestLog probe = HarvestLog.builder()
                .batchId(batchId)
                .fishTypeId(fishTypeId)
                .pondId(pondId)
                .build();
        return harvestLogRepository.findAll(Example.of(probe));
    }

    /**
     * Get harvest logs by batch ID.
     */
    public List<HarvestLog> getHarvestLogsByBatch(String batchId) {
        return harvestLogRepository.findByBatchId(batchId);
    }

    /**
     * Record a harvest event.
     * Automatically validates and recalculates rates based on cultivation type,
     * creates stock events, and handles batch closure if final harvest.
     */
    @Transactional
    public HarvestLog recordHarvest(HarvestLog harvestLog) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            throw new BusinessException("Tenant context is required");
        }

        // Validate and recalculate harvest values based on cultivation type
        validateAndRecalculateHarvest(harvestLog);

        harvestLog.setTenantId(tenantId);
        HarvestLog savedLog = harvestLogRepository.save(harvestLog);
        log.info("Recorded harvest: {} for batch: {}, pond: {}", 
            savedLog.getId(), savedLog.getBatchId(), savedLog.getPondId());

        // Create stock event for harvest
        if (savedLog.getCount() != null && savedLog.getCount() > 0) {
            createHarvestStockEvent(savedLog);
        }

        // Handle batch closure if final harvest
        if (Boolean.TRUE.equals(harvestLog.getIsFinal()) && harvestLog.getBatchId() != null) {
            handleFinalHarvest(harvestLog, tenantId);
        }

        return savedLog;
    }

    /**
     * Update an existing harvest log.
     * Re-validates and recalculates rates from the submitted values.
     */
    @Transactional
    public HarvestLog updateHarvestLog(String id, HarvestLog updated) {
        String tenantId = TenantContext.getTenantId();
        HarvestLog existing = harvestLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Harvest log not found: " + id));

        if (!existing.getTenantId().equals(tenantId)) {
            throw new BusinessException("Access denied for harvest log: " + id);
        }

        // Preserve identity fields
        updated.setId(id);
        updated.setTenantId(tenantId);

        // Re-validate and recalculate financials
        validateAndRecalculateHarvest(updated);

        HarvestLog saved = harvestLogRepository.save(updated);
        log.info("Updated harvest log: {} for tenant: {}", id, tenantId);
        return saved;
    }

    /**
     * Delete a harvest log by ID.
     */
    @Transactional
    public void deleteHarvestLog(String id) {
        String tenantId = TenantContext.getTenantId();
        HarvestLog existing = harvestLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Harvest log not found: " + id));

        if (!existing.getTenantId().equals(tenantId)) {
            throw new BusinessException("Access denied for harvest log: " + id);
        }

        harvestLogRepository.deleteById(id);
        log.info("Deleted harvest log: {} for tenant: {}", id, tenantId);
    }

    // ========== Private Helper Methods ==========

    /**
     * Validates and recalculates harvest values based on cultivation type.
     * For Shrimp: Uses basic rate directly (no discount, no commission, no rate adjustment)
     * For Fish: Applies 5% discount, adjusts rate based on ABW, applies commission
     */
    private void validateAndRecalculateHarvest(HarvestLog harvestLog) {
        if (harvestLog.getFishTypeId() == null || harvestLog.getFishTypeId().isEmpty()) {
            throw new BusinessException("Fish type is required for harvest validation");
        }

        // Get fish type to determine cultivation type
        FishType fishType = fishTypeRepository.findById(harvestLog.getFishTypeId())
                .orElseThrow(() -> new BusinessException("Fish type not found: " + harvestLog.getFishTypeId()));

        CultivationType cultivationType = fishType.getCultivationType();
        if (cultivationType == null) {
            cultivationType = CultivationType.OTHER; // Default to OTHER if not set
        }

        Double biomass = harvestLog.getBiomassWeight();
        Double basicRate = harvestLog.getBasicRate();
        Double avgWeightGrams = harvestLog.getAvgWeightGrams();

        if (biomass == null || biomass <= 0) {
            throw new BusinessException("Biomass weight must be greater than 0");
        }
        if (basicRate == null || basicRate <= 0) {
            throw new BusinessException("Basic rate must be greater than 0");
        }
        if (avgWeightGrams == null) {
            avgWeightGrams = 0.0;
        }

        // Calculate discount based on cultivation type
        double discountPercentage;
        double discountWeight;
        double actualWeight;

        if (cultivationType == CultivationType.SHRIMP) {
            // Shrimp: No discount
            discountPercentage = 0.0;
            discountWeight = 0.0;
            actualWeight = biomass;
        } else {
            // Fish: 5% discount
            discountPercentage = 0.05;
            discountWeight = biomass * discountPercentage;
            actualWeight = biomass - discountWeight;
        }

        // Calculate final rate based on cultivation type
        double finalRate;
        if (cultivationType == CultivationType.SHRIMP) {
            // Shrimp: Use basic rate directly (no adjustment)
            finalRate = basicRate;
            log.debug("Shrimp harvest - using basic rate directly: {}", basicRate);
        } else {
            // Fish: Apply rate adjustment based on average weight
            // If avgWeight < 1000g (1kg), reduce rate by 0.02 per gram below 1000g
            finalRate = basicRate;
            if (avgWeightGrams > 0 && avgWeightGrams < 1000) {
                finalRate = basicRate - (1000 - avgWeightGrams) * 0.02;
            }
            log.debug("Fish harvest - calculated rate: {} (basic: {}, avgWeight: {}g)", 
                finalRate, basicRate, avgWeightGrams);
        }

        // Calculate commission and revenue
        Double commissionRateValue = harvestLog.getCommissionRate();
        if (commissionRateValue == null) {
            commissionRateValue = 0.0;
        }
        
        double commission = (actualWeight * finalRate) * (commissionRateValue / 100);
        double revenue = (actualWeight * finalRate) - commission;

        // Set validated and recalculated values
        harvestLog.setDiscountWeight(discountWeight);
        harvestLog.setActualWeight(actualWeight);
        harvestLog.setFinalRate(finalRate);
        harvestLog.setCommission(commission);
        harvestLog.setRevenue(revenue);

        log.info("Harvest validated for {} - Type: {}, Biomass: {}kg, Discount: {}%, FinalRate: ₹{}, Revenue: ₹{}", 
            fishType.getName(), cultivationType, biomass, discountPercentage * 100, finalRate, revenue);
    }

    private void createHarvestStockEvent(HarvestLog harvestLog) {
        FishStockEvent event = FishStockEvent.builder()
                .pondId(harvestLog.getPondId())
                .batchId(harvestLog.getBatchId())
                .date(harvestLog.getDate())
                .type(FishStockEventType.HARVEST)
                .fishTypeId(harvestLog.getFishTypeId())
                .quantityDelta(-harvestLog.getCount())  // Negative delta for harvest
                .avgWeightGram(harvestLog.getAvgWeightGrams())
                .referenceId(harvestLog.getId())
                .tenantId(harvestLog.getTenantId())
                .build();

        fishStockEventRepository.save(event);
        log.debug("Created harvest stock event: {} fish from pond: {}", harvestLog.getCount(), harvestLog.getPondId());
    }

    private void handleFinalHarvest(HarvestLog harvestLog, String tenantId) {
        batchRepository.findById(harvestLog.getBatchId()).ifPresent(batch -> {
            String targetTypeId = harvestLog.getFishTypeId();

            // Mark seeds as harvested
            if (batch.getFishSeeds() != null) {
                markSeedsAsHarvested(batch, targetTypeId);
                
                // Clear pond assignments if no active seeds remain
                clearPondAssignmentsIfNeeded(batch);
                
                batchRepository.save(batch);
                log.info("Marked batch {} as final harvest for fishType: {}", 
                    batch.getId(), targetTypeId);
            }
        });
    }

    private void markSeedsAsHarvested(Batch batch, String targetTypeId) {
        batch.getFishSeeds().forEach(seed -> {
            if (targetTypeId.equalsIgnoreCase(seed.getFishTypeId())) {
                seed.setStatus("Harvested");
                log.debug("Marked seed {} as Harvested", seed.getId());
            }
        });
    }

    private void clearPondAssignmentsIfNeeded(Batch batch) {
        // Get all ponds affected by this batch
        Set<String> affectedPonds = batch.getFishSeeds().stream()
                .map(FishSeed::getPondId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // For each pond, check if any active seeds remain
        for (String pondId : affectedPonds) {
            boolean pondStillHasActiveSeeds = batch.getFishSeeds().stream()
                    .filter(s -> pondId.equals(s.getPondId()))
                    .anyMatch(s -> "Active".equalsIgnoreCase(s.getStatus()));

            if (!pondStillHasActiveSeeds) {
                clearPondBatchAssignment(pondId, batch.getId());
            }
        }
    }

    private void clearPondBatchAssignment(String pondId, String batchId) {
        pondRepository.findById(pondId).ifPresent(pond -> {
            if (batchId.equals(pond.getCurrentBatchId())) {
                pond.setCurrentBatchId(null);
                pondRepository.save(pond);
                log.info("Cleared currentBatchId from pond: {} (batch completed)", pondId);
            }
        });
    }
}
