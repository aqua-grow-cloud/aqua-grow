package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.MonitoringLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.MonitoringLogRepository;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing water quality monitoring logs.
 * Handles monitoring log creation, retrieval, and search functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MonitoringLogRepository monitoringLogRepository;
    private final BatchRepository batchRepository;

    /**
     * Creates a new monitoring log entry.
     * Sets the tenant ID from the current security context.
     *
     * @param monitoringLog the monitoring log to create
     * @return the created monitoring log with generated ID
     * @throws BusinessException if tenant context is missing
     */
    @Transactional
    public MonitoringLog createMonitoringLog(MonitoringLog monitoringLog) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        monitoringLog.setTenantId(tenantId);
        MonitoringLog saved = monitoringLogRepository.save(monitoringLog);

        log.info("Created monitoring log: {} for batch: {} on date: {} (tenant: {})",
                saved.getId(), saved.getBatchId(), saved.getDate(), tenantId);

        return saved;
    }

    /**
     * Searches monitoring logs with optional filters for batch and pond.
     * Supports pagination.
     *
     * @param batchId optional batch ID filter
     * @param pondId optional pond ID filter (finds all batches associated with the pond)
     * @param pageable pagination parameters
     * @return page of monitoring logs matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<MonitoringLog> searchMonitoringLogs(String batchId, String pondId, Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable cannot be null");

        Specification<MonitoringLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by batch ID if provided
            if (batchId != null && !batchId.isEmpty()) {
                predicates.add(cb.equal(root.get("batchId"), batchId));
            }

            // Filter by pond ID if provided
            if (pondId != null && !pondId.isEmpty()) {
                List<String> batchIds = findBatchIdsByPondId(pondId);

                if (batchIds.isEmpty()) {
                    // No batches found for this pond - return empty result
                    predicates.add(cb.equal(root.get("batchId"), "non-existent-id"));
                } else {
                    predicates.add(root.get("batchId").in(batchIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MonitoringLog> results = monitoringLogRepository.findAll(spec, pageable);

        log.debug("Found {} monitoring logs (batchId: {}, pondId: {}, page: {})",
                results.getTotalElements(), batchId, pondId, pageable.getPageNumber());

        return results;
    }

    /**
     * Retrieves all monitoring logs for a specific batch.
     *
     * @param batchId the batch ID
     * @return list of monitoring logs for the batch
     */
    @Transactional(readOnly = true)
    public List<MonitoringLog> getMonitoringLogsByBatch(String batchId) {
        List<MonitoringLog> logs = monitoringLogRepository.findByBatchId(batchId);

        log.debug("Found {} monitoring logs for batch: {}", logs.size(), batchId);

        return logs;
    }

    /**
     * Helper method to find batch IDs associated with a pond.
     * A batch is associated with a pond if any of its fish seeds are assigned to that pond.
     *
     * @param pondId the pond ID
     * @return list of batch IDs
     */
    private List<String> findBatchIdsByPondId(String pondId) {
        return batchRepository.findDistinctByFishSeedsPondId(pondId)
                .stream()
                .map(Batch::getId)
                .toList();
    }
}
