package com.aquagrow.api.controller;

import com.aquagrow.api.model.SampleLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.SampleLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sample-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SampleLogController {

    private final BatchRepository batchRepository;
    private final SampleLogRepository sampleLogRepository;

    @GetMapping
    public org.springframework.data.domain.Page<SampleLog> searchSampleLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String pondId,
            @RequestParam(required = false) String fishType,
            org.springframework.data.domain.Pageable pageable) {

        return sampleLogRepository
                .findAll((org.springframework.data.jpa.domain.Specification<SampleLog>) (root, query, cb) -> {
                    java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

                    if (batchId != null && !batchId.isEmpty()) {
                        predicates.add(cb.equal(root.get("batchId"), batchId));
                    }

                    if (fishType != null && !fishType.isEmpty()) {
                        predicates.add(cb.equal(root.get("fishType"), fishType));
                    }

                    if (pondId != null && !pondId.isEmpty()) {
                        // Determine batch IDs for this pond
                        java.util.List<String> batchIds = batchRepository.findDistinctByFishSeedsPondId(pondId)
                                .stream().map(com.aquagrow.api.model.Batch::getId).toList();

                        if (batchIds.isEmpty()) {
                            // If no batches for this pond, force empty result via impossible predicate
                            predicates.add(cb.equal(root.get("batchId"), "non-existent-id"));
                        } else {
                            predicates.add(root.get("batchId").in(batchIds));
                        }
                    }

                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                }, pageable);
    }

    @PostMapping
    public SampleLog logSample(@RequestBody SampleLog log) {
        log.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        return sampleLogRepository.save(log);
    }

    @GetMapping("/batch/{batchId}")
    public List<SampleLog> getSampleLogsByBatch(@PathVariable String batchId) {
        return sampleLogRepository.findByBatchId(batchId);
    }
}
