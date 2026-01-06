package com.aquagrow.api.controller;

import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
@Slf4j
public class DataMigrationController {

    private final BatchRepository batchRepository;
    private final HarvestLogRepository harvestLogRepository;
    private final SampleLogRepository sampleLogRepository;
    private final FishStockEventRepository fishStockEventRepository;

    @PostMapping("/populate-ledger")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('Admin')")
    public String populateLedger() {
        AtomicInteger seedCount = new AtomicInteger();
        AtomicInteger harvestCount = new AtomicInteger();
        AtomicInteger sampleCount = new AtomicInteger();

        String currentTenantId = com.aquagrow.api.security.TenantContext.getTenantId();

        // 1. Migrate Fish Seeds (Initial Stocking)
        List<Batch> batches = batchRepository.findAll();
        for (Batch batch : batches) {
            if (batch.getFishSeeds() != null) {
                for (FishSeed seed : batch.getFishSeeds()) {
                    if (seed.getId() != null && !fishStockEventRepository
                            .existsByTypeAndReferenceId(FishStockEventType.SEED, seed.getId())) {
                        FishStockEvent event = FishStockEvent.builder()
                                .pondId(seed.getPondId())
                                .batchId(batch.getId())
                                .date(seed.getDate())
                                .type(FishStockEventType.SEED)
                                .fishType(seed.getType())
                                .quantityDelta(seed.getQuantity())
                                .avgWeightGram(seed.getSizeGrams())
                                .referenceId(seed.getId())
                                .tenantId(seed.getTenantId() != null ? seed.getTenantId() : currentTenantId)
                                .build();
                        fishStockEventRepository.save(java.util.Objects.requireNonNull(event));
                        seedCount.incrementAndGet();
                    }
                }
            }
        }

        // 2. Migrate Harvest Logs
        List<HarvestLog> harvestLogs = harvestLogRepository.findAll();
        for (HarvestLog log : harvestLogs) {
            if (log.getId() != null
                    && !fishStockEventRepository.existsByTypeAndReferenceId(FishStockEventType.HARVEST, log.getId())) {
                FishStockEvent event = FishStockEvent.builder()
                        .pondId(log.getPondId())
                        .batchId(log.getBatchId())
                        .date(log.getDate())
                        .type(FishStockEventType.HARVEST)
                        .fishType(log.getFishType())
                        .quantityDelta(log.getCount() != null ? -log.getCount() : 0)
                        .avgWeightGram(log.getAvgWeightGrams())
                        .referenceId(log.getId())
                        .tenantId(log.getTenantId() != null ? log.getTenantId() : currentTenantId)
                        .build();
                fishStockEventRepository.save(java.util.Objects.requireNonNull(event));
                harvestCount.incrementAndGet();
            }
        }

        // 3. Migrate Sample Logs
        List<SampleLog> sampleLogs = sampleLogRepository.findAll();
        for (SampleLog log : sampleLogs) {
            if (log.getId() != null && log.getBatchId() != null
                    && !fishStockEventRepository.existsByTypeAndReferenceId(FishStockEventType.SAMPLING, log.getId())) {
                // Try to resolve pond ID logic (similar to SampleLogController)
                String resolvedPondId = batchRepository.findById(java.util.Objects.requireNonNull(log.getBatchId()))
                        .map(batch -> batch.getFishSeeds().stream()
                                .filter(s -> s.getType().equals(log.getFishType()))
                                .map(FishSeed::getPondId)
                                .distinct()
                                .toList())
                        .filter(list -> list.size() == 1)
                        .map(list -> list.get(0))
                        .orElse("N/A");

                FishStockEvent event = FishStockEvent.builder()
                        .pondId(resolvedPondId)
                        .batchId(log.getBatchId())
                        .date(log.getDate())
                        .type(FishStockEventType.SAMPLING)
                        .fishType(log.getFishType())
                        .quantityDelta(0)
                        .avgWeightGram(log.getAverageWeight())
                        .referenceId(log.getId())
                        .tenantId(log.getTenantId() != null ? log.getTenantId() : currentTenantId)
                        .build();
                fishStockEventRepository.save(java.util.Objects.requireNonNull(event));
                sampleCount.incrementAndGet();
            }
        }

        return String.format(
                "Migration complete: %d seeds, %d harvests, %d samples migrated back to ledger for tenant: %s.",
                seedCount.get(), harvestCount.get(), sampleCount.get(), currentTenantId);
    }
}
