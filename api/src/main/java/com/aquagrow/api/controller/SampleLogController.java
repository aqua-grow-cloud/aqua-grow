package com.aquagrow.api.controller;

import com.aquagrow.api.model.SampleLog;
import com.aquagrow.api.service.SampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sample-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SampleLogController {

    private final SampleService sampleService;

    @GetMapping
    public Page<SampleLog> searchSampleLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String pondId,
            @RequestParam(required = false) String fishTypeId,
            Pageable pageable) {
        return sampleService.searchSampleLogs(batchId, pondId, fishTypeId, pageable);
    }

    @PostMapping
    public SampleLog createSampleLog(@RequestBody SampleLog sampleLog) {
        return sampleService.createSampleLog(sampleLog);
    }

    @GetMapping("/{id}")
    public SampleLog getSampleLogById(@PathVariable String id) {
        return sampleService.getSampleLogById(id)
                .orElseThrow(() -> new com.aquagrow.api.exception.BusinessException("Sample log not found: " + id));
    }

    @GetMapping("/batch/{batchId}")
    public List<SampleLog> getSampleLogsByBatch(@PathVariable String batchId) {
        return sampleService.getSampleLogsByBatch(batchId);
    }

    @PutMapping("/{id}")
    public SampleLog updateSampleLog(@PathVariable String id, @RequestBody SampleLog updates) {
        return sampleService.updateSampleLog(id, updates);
    }

    @DeleteMapping("/{id}")
    public void deleteSampleLog(@PathVariable String id) {
        sampleService.deleteSampleLog(id);
    }

    @PostMapping("/import")
    public List<SampleLog> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        return sampleService.importFromCsv(file);
    }

    // ==================== Analytics Endpoints ====================

    @GetMapping("/batch/{batchId}/weight-trend")
    public List<Map<String, Object>> getWeightTrend(@PathVariable String batchId) {
        return sampleService.getWeightTrendForBatch(batchId);
    }

    @GetMapping("/batch/{batchId}/growth-rate")
    public Double getGrowthRate(@PathVariable String batchId) {
        return sampleService.getGrowthRateForBatch(batchId);
    }

    @GetMapping("/batch/{batchId}/latest")
    public SampleLog getLatestSample(@PathVariable String batchId) {
        return sampleService.getLatestSampleForBatch(batchId)
                .orElseThrow(() -> new com.aquagrow.api.exception.BusinessException("No samples found for batch: " + batchId));
    }

    @GetMapping("/batch/{batchId}/survival")
    public Double getAverageSurvival(@PathVariable String batchId) {
        return sampleService.getAverageSurvivalForBatch(batchId);
    }

    @GetMapping("/batch/{batchId}/biomass")
    public Double getEstimatedBiomass(@PathVariable String batchId) {
        return sampleService.getEstimatedBiomassForBatch(batchId);
    }

    @GetMapping("/date-range")
    public List<SampleLog> getSampleLogsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return sampleService.getSampleLogsByDateRange(startDate, endDate);
    }
}
