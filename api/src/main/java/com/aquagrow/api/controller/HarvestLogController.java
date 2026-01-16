package com.aquagrow.api.controller;

import com.aquagrow.api.model.HarvestLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.HarvestLogRepository;
import com.aquagrow.api.repository.PondRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/harvest-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HarvestLogController {

    private final HarvestLogRepository harvestLogRepository;
    private final BatchRepository batchRepository;
    private final PondRepository pondRepository;
    private final com.aquagrow.api.repository.FishStockEventRepository fishStockEventRepository;

    @GetMapping
    public List<HarvestLog> getAllHarvestLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String fishTypeId,
            @RequestParam(required = false) String pondId) {
        HarvestLog probe = HarvestLog.builder()
                .batchId(batchId)
                .fishTypeId(fishTypeId)
                .pondId(pondId)
                .build();
        return harvestLogRepository.findAll(Example.of(probe));
    }

    @PostMapping
    public HarvestLog recordHarvest(@RequestBody HarvestLog log) {
        log.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        HarvestLog savedLog = harvestLogRepository.save(log);

        // Create FishStockEvent for harvest (negative delta)
        if (savedLog.getCount() != null && savedLog.getCount() > 0) {
            fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                    .pondId(savedLog.getPondId())
                    .batchId(savedLog.getBatchId())
                    .date(savedLog.getDate())
                    .type(com.aquagrow.api.model.FishStockEventType.HARVEST)
                    .fishTypeId(savedLog.getFishTypeId())
                    .quantityDelta(-savedLog.getCount())
                    .avgWeightGram(savedLog.getAvgWeightGrams())
                    .referenceId(savedLog.getId())
                    .tenantId(savedLog.getTenantId())
                    .build());
        }

        // Handle batch closure if final harvest
        // Handle batch closure if final harvest
        if (Boolean.TRUE.equals(log.getIsFinal()) && log.getBatchId() != null) {
            batchRepository.findById(log.getBatchId()).ifPresent(batch -> {
                // Determine which seeds to mark as harvested.
                // If log has a fishType, mark seeds of that type.
                // If generic final harvest, mark ALL seeds (legacy behavior or full harvest).

                String targetTypeId = log.getFishTypeId();

                if (batch.getFishSeeds() != null) {
                    batch.getFishSeeds().forEach(seed -> {
                        boolean match = targetTypeId == null || targetTypeId.equalsIgnoreCase(seed.getFishTypeId());
                        if (match) {
                            seed.setStatus("Harvested");
                        }
                    });

                    // After updating statuses, let's check each pond involved in this batch
                    Set<String> affectedPonds = batch.getFishSeeds().stream()
                            .map(com.aquagrow.api.model.FishSeed::getPondId)
                            .filter(java.util.Objects::nonNull)
                            .collect(Collectors.toSet());

                    for (String pId : affectedPonds) {
                        boolean pondStillHasActiveSeeds = batch.getFishSeeds().stream()
                                .filter(s -> pId.equals(s.getPondId()))
                                .anyMatch(s -> "Active".equalsIgnoreCase(s.getStatus()));

                        if (!pondStillHasActiveSeeds) {
                            pondRepository.findById(pId).ifPresent(p -> {
                                if (batch.getId().equals(p.getCurrentBatchId())) {
                                    p.setCurrentBatchId(null);
                                    pondRepository.save(p);
                                }
                            });
                        }
                    }
                }

                batchRepository.save(batch);
            });
        }

        return savedLog;
    }

    @GetMapping("/batch/{batchId}")
    public List<HarvestLog> getHarvestLogsByBatch(@PathVariable String batchId) {
        return harvestLogRepository.findByBatchId(batchId);
    }
}
