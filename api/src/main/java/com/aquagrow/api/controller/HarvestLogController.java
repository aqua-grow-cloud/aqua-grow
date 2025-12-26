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
        HarvestLog savedLog = harvestLogRepository.save(log);

        // Handle batch closure if final harvest
        if (Boolean.TRUE.equals(log.getIsFinal()) && log.getBatchId() != null) {
            batchRepository.findById(log.getBatchId()).ifPresent(batch -> {
                batch.setStatus("Harvested");
                batchRepository.save(batch);

                // Clear currentBatchId for all ponds involved in this batch
                if (batch.getFishSeeds() != null) {
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
            });
        }

        return savedLog;
    }

    @GetMapping("/batch/{batchId}")
    public List<HarvestLog> getHarvestLogsByBatch(@PathVariable String batchId) {
        return harvestLogRepository.findByBatchId(batchId);
    }
}
