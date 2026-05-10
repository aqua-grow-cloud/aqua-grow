package com.aquagrow.api.controller;

import com.aquagrow.api.model.HarvestLog;
import com.aquagrow.api.service.HarvestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Harvest Log operations.
 * Delegates all business logic to HarvestService.
 */
@RestController
@RequestMapping("/api/harvest-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class HarvestLogController {

    private final HarvestService harvestService;

    @GetMapping
    public List<HarvestLog> getAllHarvestLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String fishTypeId,
            @RequestParam(required = false) String pondId) {
        return harvestService.getAllHarvestLogs(batchId, fishTypeId, pondId);
    }

    @PostMapping
    public HarvestLog recordHarvest(@RequestBody HarvestLog log) {
        return harvestService.recordHarvest(log);
    }

    @PutMapping("/{id}")
    public HarvestLog updateHarvestLog(@PathVariable String id, @RequestBody HarvestLog log) {
        return harvestService.updateHarvestLog(id, log);
    }

    @DeleteMapping("/{id}")
    public void deleteHarvestLog(@PathVariable String id) {
        harvestService.deleteHarvestLog(id);
    }

    @GetMapping("/batch/{batchId}")
    public List<HarvestLog> getHarvestLogsByBatch(@PathVariable String batchId) {
        return harvestService.getHarvestLogsByBatch(batchId);
    }
}
