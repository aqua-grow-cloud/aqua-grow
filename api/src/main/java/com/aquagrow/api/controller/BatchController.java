package com.aquagrow.api.controller;

import com.aquagrow.api.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Join;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import org.springframework.web.multipart.MultipartFile;
import com.aquagrow.api.dto.CsvFishSeed;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishSeed;
import com.aquagrow.api.model.Pond;
import com.aquagrow.api.model.FishType;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BatchController {

    private final BatchRepository batchRepository;
    private final com.aquagrow.api.repository.PondRepository pondRepository;
    private final com.aquagrow.api.repository.FishStockEventRepository fishStockEventRepository;
    private final com.aquagrow.api.repository.FishTypeRepository fishTypeRepository;

    @GetMapping
    public List<Batch> getAllBatches(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String pondId,
            @RequestParam(required = false) String fishTypeId) {

        return batchRepository.findAll((Specification<Batch>) (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"));
            }

            if (pondId != null && !pondId.isEmpty()) {
                Join<Object, Object> seeds = root.join("fishSeeds");
                predicates.add(criteriaBuilder.equal(seeds.get("pondId"), pondId));
            }

            if (fishTypeId != null && !fishTypeId.isEmpty()) {
                Join<Object, Object> seeds = root.join("fishSeeds");
                predicates.add(criteriaBuilder.equal(seeds.get("fishTypeId"), fishTypeId));
            }

            if (query != null) {
                query.distinct(true);
            }
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    @PostMapping
    public Batch createBatch(@RequestBody Batch batch) {
        String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();
        batch.setTenantId(tenantId);

        Batch savedBatch = batchRepository.save(batch);

        // Log SEED events for each initial fish seed
        if (savedBatch.getFishSeeds() != null) {
            savedBatch.getFishSeeds().forEach(seed -> {
                com.aquagrow.api.model.FishStockEvent event = com.aquagrow.api.model.FishStockEvent.builder()
                        .pondId(seed.getPondId())
                        .batchId(savedBatch.getId())
                        .date(seed.getDate())
                        .type(com.aquagrow.api.model.FishStockEventType.SEED)
                        .fishTypeId(seed.getFishTypeId())
                        .quantityDelta(seed.getQuantity())
                        .avgWeightGram(seed.getSizeGrams())
                        .referenceId(seed.getId())
                        .tenantId(tenantId)
                        .build();
                fishStockEventRepository.save(event);
            });
        }

        // Update first pond's currentBatchId if seeds exist
        if (savedBatch.getFishSeeds() != null && !savedBatch.getFishSeeds().isEmpty()) {
            String pondId = savedBatch.getFishSeeds().get(0).getPondId();
            if (pondId != null) {
                pondRepository.findById(pondId).ifPresent(pond -> {
                    pond.setCurrentBatchId(savedBatch.getId());
                    pondRepository.save(pond);
                });
            }
        }
        return savedBatch;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Batch> getBatchById(@PathVariable String id) {
        return batchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pond/{pondId}")
    public List<Batch> getBatchesByPond(@PathVariable String pondId) {
        return batchRepository.findDistinctByFishSeedsPondId(pondId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Batch> updateBatch(@PathVariable String id, @RequestBody Batch batchDetails) {
        return batchRepository.findById(id)
                .map(batch -> {
                    batch.setName(batchDetails.getName());
                    batch.setStockingDate(batchDetails.getStockingDate());
                    batch.setEstimatedHarvestDate(batchDetails.getEstimatedHarvestDate());

                    // Update status for all fish seeds if provided
                    String newStatus = batchDetails.getStatus(); // This is transient, but might come in JSON
                    // wait, getStatus is transient, so batchDetails.getStatus() might work if
                    // deserialized into the field,
                    // BUT Jackson won't populate a transient field unless we have a setter?
                    // No setter for status exists anymore. So batchDetails.getStatus() will likely
                    // be "Unknown" or whatever get logic is.
                    // We should accept status as separate param or check the JSON structure.
                    // Actually, for PUT, we typically send the whole object.
                    // Let's assume the UI sends "status" property. With a calculated getter,
                    // Jackson serializes it.
                    // But for deserialization, we need a setter or @JsonProperty.
                    // However, we removed the field.
                    // Let's trust that we handle status updates via the specific status endpoint or
                    // specific seed updates.
                    // Or we iterate seeds if we really want to support batch-level status update
                    // (which sets all seeds).

                    return ResponseEntity.ok(batchRepository.save(batch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/seeds")
    public ResponseEntity<Batch> addFishSeed(@PathVariable String id,
            @RequestBody com.aquagrow.api.model.FishSeed seed) {
        return batchRepository.findById(id)
                .map(batch -> {
                    String tenantId = batch.getTenantId();
                    // Ensure the seed is treated as new
                    seed.setId(null);
                    // Inherit tenantId from Batch
                    seed.setTenantId(tenantId);
                    seed.setStatus("Active"); // Default to Active

                    // Ensure the collection is initialized
                    if (batch.getFishSeeds() == null) {
                        batch.setFishSeeds(new java.util.ArrayList<>());
                    }
                    batch.getFishSeeds().add(seed);
                    Batch savedBatch = batchRepository.save(batch);

                    // Find the saved seed (to get its generated ID)
                    com.aquagrow.api.model.FishSeed savedSeed = savedBatch.getFishSeeds().stream()
                            .filter(s -> s.getFishTypeId().equals(seed.getFishTypeId())
                                    && s.getQuantity().equals(seed.getQuantity()))
                            .findFirst()
                            .orElse(seed);

                    // Create FishStockEvent for the new seed
                    fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                            .pondId(seed.getPondId())
                            .batchId(savedBatch.getId())
                            .date(seed.getDate())
                            .type(com.aquagrow.api.model.FishStockEventType.SEED)
                            .fishTypeId(seed.getFishTypeId())
                            .quantityDelta(seed.getQuantity())
                            .avgWeightGram(seed.getSizeGrams())
                            .referenceId(savedSeed.getId())
                            .tenantId(tenantId)
                            .build());

                    // Update pond currentBatchId
                    if (seed.getPondId() != null) {
                        pondRepository.findById(seed.getPondId()).ifPresent(pond -> {
                            pond.setCurrentBatchId(savedBatch.getId());
                            pondRepository.save(pond);
                        });
                    }
                    return ResponseEntity.ok(savedBatch);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // New endpoint to update status of a specific seed
    @PatchMapping("/{id}/seeds/{seedId}/status")
    public ResponseEntity<Batch> updateSeedStatus(@PathVariable String id, @PathVariable String seedId,
            @RequestParam String status) {
        return batchRepository.findById(id)
                .map(batch -> {
                    batch.getFishSeeds().stream()
                            .filter(s -> s.getId() != null && s.getId().equals(seedId)) // Filter by ID
                            // The seedId in fishSeeds might be null if not persisted yet? No, they are
                            // persisted.
                            // Or we can just equals check.
                            .findFirst()
                            .ifPresent(s -> s.setStatus(status));

                    return ResponseEntity.ok(batchRepository.save(batch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Batch> updateBatchStatus(@PathVariable String id, @RequestParam String status) {
        return batchRepository.findById(id)
                .map(batch -> {
                    // Setting batch status implies setting status for ALL seeds
                    if (batch.getFishSeeds() != null) {
                        batch.getFishSeeds().forEach(seed -> seed.setStatus(status));
                    }
                    return ResponseEntity.ok(batchRepository.save(batch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/seeds/{seedId}/move")
    public ResponseEntity<Batch> moveFishSeed(
            @PathVariable String id,
            @PathVariable String seedId,
            @RequestParam String targetPondId,
            @RequestParam(required = false) Integer quantity) {
        return batchRepository.findById(id)
                .map(batch -> {
                    com.aquagrow.api.model.FishSeed sourceSeed = batch.getFishSeeds().stream()
                            .filter(s -> s.getId() != null && s.getId().equals(seedId))
                            .findFirst()
                            .orElse(null);

                    if (sourceSeed == null) {
                        return ResponseEntity.notFound().<Batch>build();
                    }

                    int moveQty = (quantity != null) ? quantity : sourceSeed.getQuantity();

                    if (moveQty > sourceSeed.getQuantity() || moveQty <= 0) {
                        return ResponseEntity.badRequest().<Batch>build();
                    }

                    String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();
                    String originalPondId = sourceSeed.getPondId();

                    if (moveQty == sourceSeed.getQuantity()) {
                        // Full move
                        sourceSeed.setPondId(targetPondId);
                    } else {
                        // Partial move - split the seed
                        sourceSeed.setQuantity(sourceSeed.getQuantity() - moveQty);

                        com.aquagrow.api.model.FishSeed newSeed = com.aquagrow.api.model.FishSeed.builder()
                                .pondId(targetPondId)
                                .date(sourceSeed.getDate())
                                .fishTypeId(sourceSeed.getFishTypeId())
                                .quantity(moveQty)
                                .sizeInches(sourceSeed.getSizeInches())
                                .sizeGrams(sourceSeed.getSizeGrams())
                                .unitPrice(sourceSeed.getUnitPrice())
                                .vendor(sourceSeed.getVendor())
                                .tenantId(tenantId)
                                .status(sourceSeed.getStatus())
                                .build();

                        batch.getFishSeeds().add(newSeed);
                    }

                    Batch savedBatch = batchRepository.save(batch);

                    // Create TRANSFER events
                    // 1. Out of source pond
                    fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                            .pondId(originalPondId)
                            .batchId(savedBatch.getId())
                            .date(LocalDate.now()) // Transfer happens now
                            .type(com.aquagrow.api.model.FishStockEventType.TRANSFER_OUT)
                            .fishTypeId(sourceSeed.getFishTypeId())
                            .quantityDelta(-moveQty)
                            .avgWeightGram(sourceSeed.getSizeGrams())
                            .referenceId(seedId)
                            .tenantId(tenantId)
                            .build());

                    // 2. Into target pond
                    fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                            .pondId(targetPondId)
                            .batchId(savedBatch.getId())
                            .date(LocalDate.now())
                            .type(com.aquagrow.api.model.FishStockEventType.TRANSFER_IN)
                            .fishTypeId(sourceSeed.getFishTypeId())
                            .quantityDelta(moveQty)
                            .avgWeightGram(sourceSeed.getSizeGrams())
                            .referenceId(seedId)
                            .tenantId(tenantId)
                            .build());

                    // Update pond currentBatchId for target pond
                    pondRepository.findById(targetPondId).ifPresent(pond -> {
                        pond.setCurrentBatchId(savedBatch.getId());
                        pondRepository.save(pond);
                    });

                    return ResponseEntity.ok(savedBatch);
                })
                .orElse(ResponseEntity.notFound().<Batch>build());
    }

    @PostMapping("/import")
    public List<Batch> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();
        List<Batch> updatedBatches = new ArrayList<>();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<CsvFishSeed> csvToBean = new CsvToBeanBuilder<CsvFishSeed>(reader)
                    .withType(CsvFishSeed.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<CsvFishSeed> csvSeeds = csvToBean.parse();
            for (CsvFishSeed csv : csvSeeds) {
                LocalDate eventDate = parseDate(csv.getDate());

                // 1. Get or create Batch
                Batch batch = batchRepository.findByName(csv.getBatchName())
                        .orElseGet(() -> {
                            Batch newBatch = Batch.builder()
                                    .name(csv.getBatchName())
                                    .tenantId(tenantId)
                                    .stockingDate(eventDate)
                                    .build();
                            return batchRepository.save(newBatch);
                        });

                // 2. Get or create Pond
                Pond pond = pondRepository.findByName(csv.getPondName())
                        .orElseGet(() -> {
                            Pond newPond = Pond.builder()
                                    .name(csv.getPondName())
                                    .tenantId(tenantId)
                                    .status("Active")
                                    .build();
                            return pondRepository.save(newPond);
                        });

                // 3. Get or create FishType
                FishType fishType = fishTypeRepository.findByName(csv.getType())
                        .orElseGet(() -> {
                            FishType newType = FishType.builder()
                                    .name(csv.getType())
                                    .tenantId(tenantId)
                                    .build();
                            return fishTypeRepository.save(newType);
                        });

                // 4. Create FishSeed
                FishSeed seed = FishSeed.builder()
                        .date(eventDate)
                        .fishTypeId(fishType.getId())
                        .pondId(pond.getId())
                        .quantity(csv.getQuantity())
                        .sizeInches(csv.getSizeInches())
                        .sizeGrams(csv.getSizeGrams())
                        .tenantId(tenantId)
                        .status("Active")
                        .build();

                if (batch.getFishSeeds() == null) {
                    batch.setFishSeeds(new ArrayList<>());
                }
                batch.getFishSeeds().add(seed);
                Batch savedBatch = batchRepository.save(batch);

                // Get the persisted seed to get its generated ID
                FishSeed savedSeed = savedBatch.getFishSeeds().stream()
                        .filter(s -> s.getFishTypeId().equals(seed.getFishTypeId())
                                && s.getQuantity().equals(seed.getQuantity())
                                && s.getDate().equals(seed.getDate()))
                        .findFirst()
                        .orElse(seed);

                // 5. Create FishStockEvent
                fishStockEventRepository.save(com.aquagrow.api.model.FishStockEvent.builder()
                        .pondId(pond.getId())
                        .batchId(savedBatch.getId())
                        .date(seed.getDate())
                        .type(com.aquagrow.api.model.FishStockEventType.SEED)
                        .fishTypeId(fishType.getId())
                        .quantityDelta(seed.getQuantity())
                        .avgWeightGram(seed.getSizeGrams())
                        .referenceId(savedSeed.getId())
                        .tenantId(tenantId)
                        .build());

                // 6. Update pond currentBatchId
                pond.setCurrentBatchId(savedBatch.getId());
                pondRepository.save(pond);

                if (updatedBatches.stream().noneMatch(b -> b.getId().equals(savedBatch.getId()))) {
                    updatedBatches.add(savedBatch);
                }
            }
        }
        return updatedBatches;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }

        String[] formats = {
                "yyyy-MM-dd", "MM/dd/yyyy", "MMMM d, yyyy", "d-MMM-yyyy", "dd-MM-yyyy"
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
