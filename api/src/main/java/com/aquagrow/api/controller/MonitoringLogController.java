package com.aquagrow.api.controller;

import java.util.Objects;

import com.aquagrow.api.model.MonitoringLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.MonitoringLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MonitoringLogController {

    private final BatchRepository batchRepository;
    private final MonitoringLogRepository monitoringLogRepository;

    @GetMapping
    public org.springframework.data.domain.Page<MonitoringLog> searchMonitoringLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String pondId,
            org.springframework.data.domain.Pageable pageable) {

        return monitoringLogRepository
                .findAll((org.springframework.data.jpa.domain.Specification<MonitoringLog>) (root, query, cb) -> {
                    java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

                    if (batchId != null && !batchId.isEmpty()) {
                        predicates.add(cb.equal(root.get("batchId"), batchId));
                    }

                    if (pondId != null && !pondId.isEmpty()) {
                        java.util.List<String> batchIds = batchRepository.findDistinctByFishSeedsPondId(pondId)
                                .stream().map(com.aquagrow.api.model.Batch::getId).toList();

                        if (batchIds.isEmpty()) {
                            predicates.add(cb.equal(root.get("batchId"), "non-existent-id"));
                        } else {
                            predicates.add(root.get("batchId").in(batchIds));
                        }
                    }

                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                }, Objects.requireNonNull(pageable));
    }

    @PostMapping
    public MonitoringLog logMonitoring(@RequestBody MonitoringLog log) {
        log.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        return monitoringLogRepository.save(log);
    }

    @GetMapping("/batch/{batchId}")
    public List<MonitoringLog> getMonitoringLogsByBatch(@PathVariable String batchId) {
        return monitoringLogRepository.findByBatchId(batchId);
    }
}
