package com.aquagrow.api.controller;

import com.aquagrow.api.model.MonitoringLog;
import com.aquagrow.api.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for water quality monitoring logs.
 * Delegates all business logic to MonitoringService.
 */
@RestController
@RequestMapping("/api/monitoring-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class MonitoringLogController {

    private final MonitoringService monitoringService;

    @GetMapping
    public Page<MonitoringLog> searchMonitoringLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String pondId,
            Pageable pageable) {
        return monitoringService.searchMonitoringLogs(batchId, pondId, pageable);
    }

    @PostMapping
    public MonitoringLog logMonitoring(@RequestBody MonitoringLog monitoringLog) {
        return monitoringService.createMonitoringLog(monitoringLog);
    }

    @GetMapping("/batch/{batchId}")
    public List<MonitoringLog> getMonitoringLogsByBatch(@PathVariable String batchId) {
        return monitoringService.getMonitoringLogsByBatch(batchId);
    }
}
