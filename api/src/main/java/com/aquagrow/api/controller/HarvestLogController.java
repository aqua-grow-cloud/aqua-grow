package com.aquagrow.api.controller;

import com.aquagrow.api.model.HarvestLog;
import com.aquagrow.api.repository.HarvestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/harvest-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HarvestLogController {

    private final HarvestLogRepository harvestLogRepository;

    @GetMapping
    public List<HarvestLog> getAllHarvestLogs() {
        return harvestLogRepository.findAll();
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
