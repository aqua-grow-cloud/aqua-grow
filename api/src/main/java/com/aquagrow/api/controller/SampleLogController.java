package com.aquagrow.api.controller;

import com.aquagrow.api.model.SampleLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.SampleLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import org.springframework.web.multipart.MultipartFile;
import com.aquagrow.api.dto.CsvSampleLog;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishType;
import com.aquagrow.api.model.Pond;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

@RestController
@RequestMapping("/api/sample-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SampleLogController {

    private final BatchRepository batchRepository;
    private final SampleLogRepository sampleLogRepository;
    private final com.aquagrow.api.repository.PondRepository pondRepository;
    private final com.aquagrow.api.repository.FishTypeRepository fishTypeRepository;
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

    @PostMapping("/import")
    public List<SampleLog> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();
        List<SampleLog> savedLogs = new ArrayList<>();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<CsvSampleLog> csvToBean = new CsvToBeanBuilder<CsvSampleLog>(reader)
                    .withType(CsvSampleLog.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<CsvSampleLog> csvLogs = csvToBean.parse();
            for (CsvSampleLog csv : csvLogs) {
                LocalDate eventDate = parseDate(csv.getDate());

                // 1. Resolve Batch
                Batch batch = batchRepository.findByName(csv.getBatchName())
                        .orElseGet(() -> batchRepository.save(Batch.builder()
                                .name(csv.getBatchName())
                                .tenantId(tenantId)
                                .stockingDate(eventDate)
                                .build()));

                // 2. Resolve Pond
                Pond pond = pondRepository.findByName(csv.getPondName())
                        .orElseGet(() -> pondRepository.save(Pond.builder()
                                .name(csv.getPondName())
                                .tenantId(tenantId)
                                .status("Active")
                                .build()));

                // 3. Resolve FishType
                FishType fishType = fishTypeRepository.findByName(csv.getFishType())
                        .orElseGet(() -> fishTypeRepository.save(FishType.builder()
                                .name(csv.getFishType())
                                .tenantId(tenantId)
                                .build()));

                // 4. Create SampleLog
                SampleLog log = SampleLog.builder()
                        .batchId(batch.getId())
                        .fishTypeId(fishType.getId())
                        .date(eventDate)
                        .sampleCount(csv.getPieces())
                        .totalSampleWeight(csv.getTotalWeight())
                        .averageWeight(csv.getWeightPerUnit())
                        .tenantId(tenantId)
                        .build();

                SampleLog savedLog = sampleLogRepository.save(log);
                savedLogs.add(savedLog);

                // 5. Create FishStockEvent (SAMPLING)
                fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                        .pondId(pond.getId())
                        .batchId(batch.getId())
                        .date(eventDate)
                        .type(com.aquagrow.api.model.FishStockEventType.SAMPLING)
                        .fishTypeId(fishType.getId())
                        .quantityDelta(0)
                        .avgWeightGram(savedLog.getAverageWeight())
                        .referenceId(savedLog.getId())
                        .tenantId(tenantId)
                        .build());
            }
        }
        return savedLogs;
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
