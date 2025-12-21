package com.aquagrow.api.controller;

import com.aquagrow.api.model.SampleLog;
import com.aquagrow.api.repository.SampleLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sample-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SampleLogController {

    private final SampleLogRepository sampleLogRepository;

    @GetMapping
    public List<SampleLog> getAllSampleLogs() {
        return sampleLogRepository.findAll();
    }

    @PostMapping
    public SampleLog logSample(@RequestBody SampleLog log) {
        return sampleLogRepository.save(log);
    }

    @GetMapping("/batch/{batchId}")
    public List<SampleLog> getSampleLogsByBatch(@PathVariable String batchId) {
        return sampleLogRepository.findByBatchId(batchId);
    }
}
