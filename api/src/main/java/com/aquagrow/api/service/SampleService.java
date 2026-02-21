package com.aquagrow.api.service;

import com.aquagrow.api.dto.CsvSampleLog;
import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import com.aquagrow.api.security.TenantContext;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleService {

    private final SampleLogRepository sampleLogRepository;
    private final BatchRepository batchRepository;
    private final PondRepository pondRepository;
    private final FishTypeRepository fishTypeRepository;
    private final FishStockEventRepository fishStockEventRepository;

    /**
     * Create a new sample log entry
     */
    @Transactional
    public SampleLog createSampleLog(SampleLog sampleLog) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        validateSampleLog(sampleLog);

        sampleLog.setTenantId(tenantId);
        SampleLog saved = sampleLogRepository.save(sampleLog);

        // Create stock event for sampling
        createStockEventForSample(saved, tenantId);

        log.info("Created sample log: {} for batch: {}", saved.getId(), saved.getBatchId());
        return saved;
    }

    /**
     * Get all sample logs with pagination
     */
    @Transactional(readOnly = true)
    public Page<SampleLog> getAllSampleLogs(Pageable pageable) {
        return sampleLogRepository.findAll(pageable);
    }

    /**
     * Get sample log by ID
     */
    @Transactional(readOnly = true)
    public Optional<SampleLog> getSampleLogById(String id) {
        return sampleLogRepository.findById(id);
    }

    /**
     * Get sample logs by batch ID
     */
    @Transactional(readOnly = true)
    public List<SampleLog> getSampleLogsByBatch(String batchId) {
        return sampleLogRepository.findByBatchId(batchId);
    }

    /**
     * Search sample logs with filters (batch, pond, fishType)
     */
    @Transactional(readOnly = true)
    public Page<SampleLog> searchSampleLogs(String batchId, String pondId, String fishTypeId, Pageable pageable) {
        Specification<SampleLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchId != null && !batchId.isEmpty()) {
                predicates.add(cb.equal(root.get("batchId"), batchId));
            }

            if (fishTypeId != null && !fishTypeId.isEmpty()) {
                predicates.add(cb.equal(root.get("fishTypeId"), fishTypeId));
            }

            if (pondId != null && !pondId.isEmpty()) {
                // Find batches associated with this pond
                List<String> batchIds = batchRepository.findDistinctByFishSeedsPondId(pondId)
                        .stream()
                        .map(Batch::getId)
                        .toList();

                if (batchIds.isEmpty()) {
                    // Force empty result if no batches for this pond
                    predicates.add(cb.equal(root.get("batchId"), "non-existent-id"));
                } else {
                    predicates.add(root.get("batchId").in(batchIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sampleLogRepository.findAll(spec, pageable);
    }

    /**
     * Update sample log
     */
    @Transactional
    public SampleLog updateSampleLog(String id, SampleLog updates) {
        SampleLog existing = sampleLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Sample log not found: " + id));

        // Update fields with validation
        if (updates.getBatchId() != null) existing.setBatchId(updates.getBatchId());
        if (updates.getFishTypeId() != null) existing.setFishTypeId(updates.getFishTypeId());
        if (updates.getDate() != null) existing.setDate(updates.getDate());
        
        if (updates.getSampleCount() != null) {
            if (updates.getSampleCount() <= 0) {
                throw new BusinessException("Sample count must be greater than zero");
            }
            existing.setSampleCount(updates.getSampleCount());
        }
        
        if (updates.getTotalSampleWeight() != null) {
            if (updates.getTotalSampleWeight() <= 0) {
                throw new BusinessException("Total sample weight must be greater than zero");
            }
            existing.setTotalSampleWeight(updates.getTotalSampleWeight());
        }
        
        if (updates.getAverageWeight() != null) {
            if (updates.getAverageWeight() <= 0) {
                throw new BusinessException("Average weight must be greater than zero");
            }
            existing.setAverageWeight(updates.getAverageWeight());
        }
        
        if (updates.getAverageLength() != null) existing.setAverageLength(updates.getAverageLength());
        
        if (updates.getSurvivalEstimate() != null) {
            if (updates.getSurvivalEstimate() < 0 || updates.getSurvivalEstimate() > 100) {
                throw new BusinessException("Survival estimate must be between 0 and 100");
            }
            existing.setSurvivalEstimate(updates.getSurvivalEstimate());
        }

        SampleLog saved = sampleLogRepository.save(existing);
        log.info("Updated sample log: {}", id);
        return saved;
    }

    /**
     * Delete sample log
     */
    @Transactional
    public void deleteSampleLog(String id) {
        if (!sampleLogRepository.existsById(id)) {
            throw new BusinessException("Sample log not found: " + id);
        }
        sampleLogRepository.deleteById(id);
        log.info("Deleted sample log: {}", id);
    }

    /**
     * Import sample logs from CSV file
     */
    @Transactional
    public List<SampleLog> importFromCsv(MultipartFile file) throws Exception {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }

        List<SampleLog> savedLogs = new ArrayList<>();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<CsvSampleLog> csvToBean = new CsvToBeanBuilder<CsvSampleLog>(reader)
                    .withType(CsvSampleLog.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<CsvSampleLog> csvLogs = csvToBean.parse();

            for (CsvSampleLog csv : csvLogs) {
                LocalDate eventDate = parseDate(csv.getDate());

                // Resolve or create entities
                Batch batch = resolveOrCreateBatch(csv.getBatchName(), eventDate, tenantId);
                Pond pond = resolveOrCreatePond(csv.getPondName(), tenantId);
                FishType fishType = resolveOrCreateFishType(csv.getFishType(), tenantId);

                // Create sample log
                SampleLog sampleLog = SampleLog.builder()
                        .batchId(batch.getId())
                        .fishTypeId(fishType.getId())
                        .date(eventDate)
                        .sampleCount(csv.getPieces())
                        .totalSampleWeight(csv.getTotalWeight())
                        .averageWeight(csv.getWeightPerUnit())
                        .tenantId(tenantId)
                        .build();

                SampleLog saved = sampleLogRepository.save(sampleLog);
                savedLogs.add(saved);

                // Create stock event
                createStockEventForCsvImport(saved, pond.getId(), tenantId);
            }
        }

        log.info("Imported {} sample logs from CSV", savedLogs.size());
        return savedLogs;
    }

    /**
     * Get average weight trend for a batch
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeightTrendForBatch(String batchId) {
        List<SampleLog> samples = sampleLogRepository.findByBatchId(batchId);

        return samples.stream()
                .filter(s -> s.getAverageWeight() != null)
                .sorted(Comparator.comparing(SampleLog::getDate))
                .map(s -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", s.getDate());
                    point.put("averageWeight", s.getAverageWeight());
                    point.put("sampleCount", s.getSampleCount());
                    return point;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get growth rate (grams per day) for a batch
     */
    @Transactional(readOnly = true)
    public Double getGrowthRateForBatch(String batchId) {
        List<SampleLog> samples = sampleLogRepository.findByBatchId(batchId);

        if (samples.size() < 2) {
            return 0.0;
        }

        samples.sort(Comparator.comparing(SampleLog::getDate));

        SampleLog first = samples.get(0);
        SampleLog last = samples.get(samples.size() - 1);

        if (first.getAverageWeight() == null || last.getAverageWeight() == null) {
            return 0.0;
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(first.getDate(), last.getDate());
        if (daysBetween == 0) {
            return 0.0;
        }

        double weightGain = last.getAverageWeight() - first.getAverageWeight();
        return weightGain / daysBetween;
    }

    /**
     * Get latest sample log for a batch
     */
    @Transactional(readOnly = true)
    public Optional<SampleLog> getLatestSampleForBatch(String batchId) {
        List<SampleLog> samples = sampleLogRepository.findByBatchId(batchId);
        return samples.stream()
                .max(Comparator.comparing(SampleLog::getDate));
    }

    /**
     * Get average survival estimate for a batch
     */
    @Transactional(readOnly = true)
    public Double getAverageSurvivalForBatch(String batchId) {
        List<SampleLog> samples = sampleLogRepository.findByBatchId(batchId);

        return samples.stream()
                .filter(s -> s.getSurvivalEstimate() != null)
                .mapToDouble(SampleLog::getSurvivalEstimate)
                .average()
                .orElse(0.0);
    }

    /**
     * Get sample logs within date range
     */
    @Transactional(readOnly = true)
    public List<SampleLog> getSampleLogsByDateRange(LocalDate startDate, LocalDate endDate) {
        return sampleLogRepository.findAll().stream()
                .filter(s -> !s.getDate().isBefore(startDate) && !s.getDate().isAfter(endDate))
                .sorted(Comparator.comparing(SampleLog::getDate))
                .collect(Collectors.toList());
    }

    /**
     * Calculate estimated biomass for batch (current stock × average weight)
     */
    @Transactional(readOnly = true)
    public Double getEstimatedBiomassForBatch(String batchId) {
        Optional<SampleLog> latestSample = getLatestSampleForBatch(batchId);
        if (latestSample.isEmpty() || latestSample.get().getAverageWeight() == null) {
            return 0.0;
        }

        // Get current stock for batch
        List<FishStockEvent> events = fishStockEventRepository.findByBatchId(batchId);
        int currentStock = events.stream()
                .mapToInt(FishStockEvent::getQuantityDelta)
                .sum();

        double avgWeightGrams = latestSample.get().getAverageWeight();
        return (currentStock * avgWeightGrams) / 1000.0; // Convert to kg
    }

    // ==================== Private Helper Methods ====================

    private void validateSampleLog(SampleLog sampleLog) {
        if (sampleLog.getBatchId() == null || sampleLog.getBatchId().isEmpty()) {
            throw new BusinessException("Batch ID is required");
        }

        if (sampleLog.getDate() == null) {
            throw new BusinessException("Sample date is required");
        }

        if (sampleLog.getSampleCount() != null && sampleLog.getSampleCount() <= 0) {
            throw new BusinessException("Sample count must be greater than zero");
        }

        if (sampleLog.getTotalSampleWeight() != null && sampleLog.getTotalSampleWeight() <= 0) {
            throw new BusinessException("Total sample weight must be greater than zero");
        }

        if (sampleLog.getAverageWeight() != null && sampleLog.getAverageWeight() <= 0) {
            throw new BusinessException("Average weight must be greater than zero");
        }

        if (sampleLog.getSurvivalEstimate() != null &&
                (sampleLog.getSurvivalEstimate() < 0 || sampleLog.getSurvivalEstimate() > 100)) {
            throw new BusinessException("Survival estimate must be between 0 and 100");
        }
    }

    private void createStockEventForSample(SampleLog sampleLog, String tenantId) {
        // Try to resolve pond ID for this batch + fish type combination
        String resolvedPondId = batchRepository.findById(sampleLog.getBatchId())
                .map(batch -> batch.getFishSeeds().stream()
                        .filter(s -> s.getFishTypeId().equals(sampleLog.getFishTypeId()))
                        .map(FishSeed::getPondId)
                        .distinct()
                        .toList())
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0))
                .orElse("N/A"); // Default if spans multiple ponds

        FishStockEvent stockEvent = FishStockEvent.builder()
                .pondId(resolvedPondId)
                .batchId(sampleLog.getBatchId())
                .date(sampleLog.getDate())
                .type(FishStockEventType.SAMPLING)
                .fishTypeId(sampleLog.getFishTypeId())
                .quantityDelta(0) // Sampling doesn't change quantity
                .avgWeightGram(sampleLog.getAverageWeight())
                .referenceId(sampleLog.getId())
                .tenantId(tenantId)
                .build();

        fishStockEventRepository.save(stockEvent);
        log.debug("Created stock event for sample log: {}", sampleLog.getId());
    }

    private void createStockEventForCsvImport(SampleLog sampleLog, String pondId, String tenantId) {
        FishStockEvent stockEvent = FishStockEvent.builder()
                .pondId(pondId)
                .batchId(sampleLog.getBatchId())
                .date(sampleLog.getDate())
                .type(FishStockEventType.SAMPLING)
                .fishTypeId(sampleLog.getFishTypeId())
                .quantityDelta(0)
                .avgWeightGram(sampleLog.getAverageWeight())
                .referenceId(sampleLog.getId())
                .tenantId(tenantId)
                .build();

        fishStockEventRepository.save(stockEvent);
    }

    private Batch resolveOrCreateBatch(String batchName, LocalDate stockingDate, String tenantId) {
        return batchRepository.findByName(batchName)
                .orElseGet(() -> batchRepository.save(Batch.builder()
                        .name(batchName)
                        .tenantId(tenantId)
                        .stockingDate(stockingDate)
                        .build()));
    }

    private Pond resolveOrCreatePond(String pondName, String tenantId) {
        return pondRepository.findByName(pondName)
                .orElseGet(() -> pondRepository.save(Pond.builder()
                        .name(pondName)
                        .tenantId(tenantId)
                        .status("Active")
                        .build()));
    }

    private FishType resolveOrCreateFishType(String fishTypeName, String tenantId) {
        return fishTypeRepository.findByName(fishTypeName)
                .orElseGet(() -> fishTypeRepository.save(FishType.builder()
                        .name(fishTypeName)
                        .tenantId(tenantId)
                        .build()));
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }

        String[] formats = {
                "yyyy-MM-dd", "MM/dd/yyyy", "MMMM d, yyyy", "d-MMM-yyyy", "MMM d, yyyy", "dd-MM-yyyy"
        };

        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(format, Locale.ENGLISH));
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now();
    }
}
