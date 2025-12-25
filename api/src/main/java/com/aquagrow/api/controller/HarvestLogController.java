package com.aquagrow.api.controller;

import com.aquagrow.api.model.HarvestLog;
import com.aquagrow.api.repository.HarvestLogRepository;
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

    @GetMapping
    public List<HarvestLog> getAllHarvestLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String fishType) {
        HarvestLog probe = HarvestLog.builder()
                .batchId(batchId)
                .fishType(fishType)
                .build();
        return harvestLogRepository.findAll(Example.of(probe));
    }

    @PostMapping
    public HarvestLog recordHarvest(@RequestBody HarvestLog log) {
        return harvestLogRepository.save(log);
    }

    @GetMapping("/batch/{batchId}")
    public List<HarvestLog> getHarvestLogsByBatch(@PathVariable String batchId) {
        return harvestLogRepository.findByBatchId(batchId);
    }
}
