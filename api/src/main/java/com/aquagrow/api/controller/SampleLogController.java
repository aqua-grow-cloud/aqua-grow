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
    private final com.aquagrow.api.repository.FishStockEventRepository fishStockEventRepository;

    @GetMapping
    public org.springframework.data.domain.Page<SampleLog> searchSampleLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String pondId,
            @RequestParam(required = false) String fishTypeId,
            org.springframework.data.domain.Pageable pageable) {

        return sampleLogRepository
                .findAll((org.springframework.data.jpa.domain.Specification<SampleLog>) (root, query, cb) -> {
                    java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

                    if (batchId != null && !batchId.isEmpty()) {
                        predicates.add(cb.equal(root.get("batchId"), batchId));
                    }

                    if (fishTypeId != null && !fishTypeId.isEmpty()) {
                        predicates.add(cb.equal(root.get("fishTypeId"), fishTypeId));
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
        String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();
        log.setTenantId(tenantId);
        SampleLog savedLog = sampleLogRepository.save(log);

        // Try to find the pondId for this event.
        // If the batch only has this fishType in one pond, we use that.
        String resolvedPondId = batchRepository.findById(savedLog.getBatchId())
                .map(batch -> batch.getFishSeeds().stream()
                        .filter(s -> s.getFishTypeId().equals(savedLog.getFishTypeId()))
                        .map(com.aquagrow.api.model.FishSeed::getPondId)
                        .distinct()
                        .toList())
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0))
                .orElse("N/A"); // Default to N/A or just don't set pondId if it spans multiple

        fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                .pondId(resolvedPondId)
                .batchId(savedLog.getBatchId())
                .date(savedLog.getDate())
                .type(com.aquagrow.api.model.FishStockEventType.SAMPLING)
                .fishTypeId(savedLog.getFishTypeId())
                .quantityDelta(0) // Sampling usually doesn't change quantity
                .avgWeightGram(savedLog.getAverageWeight())
                .referenceId(savedLog.getId())
                .tenantId(tenantId)
                .build());

        return savedLog;
    }

    @GetMapping("/batch/{batchId}")
    public List<SampleLog> getSampleLogsByBatch(@PathVariable String batchId) {
        return sampleLogRepository.findByBatchId(batchId);
    }
}
