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
     * Automatically creates stock events and handles batch closure if final harvest.
     */
    @Transactional
    public HarvestLog recordHarvest(HarvestLog harvestLog) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            throw new BusinessException("Tenant context is required");
        }

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

    // ========== Private Helper Methods ==========

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
                    batch.getId(), targetTypeId != null ? targetTypeId : "ALL");
            }
        });
    }

    private void markSeedsAsHarvested(Batch batch, String targetTypeId) {
        batch.getFishSeeds().forEach(seed -> {
            boolean match = targetTypeId == null || targetTypeId.equalsIgnoreCase(seed.getFishTypeId());
            if (match) {
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
