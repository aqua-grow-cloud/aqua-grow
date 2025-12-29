package com.aquagrow.api.controller;

import com.aquagrow.api.model.HarvestLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.HarvestLogRepository;
import com.aquagrow.api.repository.PondRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/harvest-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HarvestLogController {

    private final HarvestLogRepository harvestLogRepository;
    private final BatchRepository batchRepository;
    private final PondRepository pondRepository;

    @GetMapping
    public List<HarvestLog> getAllHarvestLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String fishType,
            @RequestParam(required = false) String pondId) {
        HarvestLog probe = HarvestLog.builder()
                .batchId(batchId)
                .fishType(fishType)
                .pondId(pondId)
                .build();
        return harvestLogRepository.findAll(Example.of(probe));
    }

    @PostMapping
    public HarvestLog recordHarvest(@RequestBody HarvestLog log) {
        log.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        HarvestLog savedLog = harvestLogRepository.save(log);

        // Handle batch closure if final harvest
        // Handle batch closure if final harvest
        if (Boolean.TRUE.equals(log.getIsFinal()) && log.getBatchId() != null) {
            batchRepository.findById(log.getBatchId()).ifPresent(batch -> {
                // Determine which seeds to mark as harvested.
                // If log has a fishType, mark seeds of that type.
                // If generic final harvest, mark ALL seeds (legacy behavior or full harvest).

                String targetType = log.getFishType();
                boolean allClosed = true;

                if (batch.getFishSeeds() != null) {
                    batch.getFishSeeds().forEach(seed -> {
                        boolean match = targetType == null || targetType.equalsIgnoreCase(seed.getType());
                        if (match) {
                            seed.setStatus("Harvested");
                        }
                    });

                    // Check if *all* are now harvested to clear pond status
                    // NOTE: getStatus() is calculated from seeds now.
                    // We need to re-save to make the calculated status effectively "Completed" for
                    // the batch
                    // But we manually check ponds here

                    // If ALL seeds for a specific pond are harvested, we can free that pond?
                    // Or only free pond if ALL seeds in batch are done?
                    // User said: "if all the FishSeeds are completed(harested) then the batch is
                    // completed."
                    // So we only clear currentBatchId from ponds when the ENTIRE batch is closed
                    // (i.e. all seeds harvested).

                    if (batch.getFishSeeds().stream().allMatch(s -> "Harvested".equalsIgnoreCase(s.getStatus())
                            || "Completed".equalsIgnoreCase(s.getStatus()))) {
                        batch.getFishSeeds().forEach(seed -> {
                            if (seed.getPondId() != null) {
                                pondRepository.findById(seed.getPondId()).ifPresent(pond -> {
                                    if (batch.getId().equals(pond.getCurrentBatchId())) {
                                        pond.setCurrentBatchId(null);
                                        pondRepository.save(pond);
                                    }
                                });
                            }
                        });
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
