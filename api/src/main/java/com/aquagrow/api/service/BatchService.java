package com.aquagrow.api.service;

import com.aquagrow.api.dto.CsvFishSeed;
import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.persistence.criteria.Join;

/**
 * Service layer for Batch operations.
 * Encapsulates business logic for batch management, fish seed operations, and stock events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final BatchRepository batchRepository;
    private final PondRepository pondRepository;
    private final FishStockEventRepository fishStockEventRepository;
    private final FishTypeRepository fishTypeRepository;

    /**
     * Get all batches with optional filtering by name, pond, or fish type.
     */
    public List<Batch> getAllBatches(String name, String pondId, String fishTypeId) {
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

    /**
     * Create a new batch with initial fish seeds.
     * Automatically creates stock events and updates pond assignments.
     */
    @Transactional
    public Batch createBatch(Batch batch) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            throw new BusinessException("Tenant context is required");
        }

        batch.setTenantId(tenantId);

        // Set tenant ID for all fish seeds
        if (batch.getFishSeeds() != null) {
            batch.getFishSeeds().forEach(seed -> seed.setTenantId(tenantId));
        }

        Batch savedBatch = batchRepository.save(batch);
        log.info("Created batch: {} for tenant: {}", savedBatch.getId(), tenantId);

        // Create stock events for initial fish seeds
        if (savedBatch.getFishSeeds() != null) {
            savedBatch.getFishSeeds().forEach(seed -> {
                createStockEvent(savedBatch.getId(), seed, FishStockEventType.SEED, seed.getQuantity(), tenantId);
            });
        }

        // Update first pond's currentBatchId
        updatePondBatchAssignment(savedBatch);

        return savedBatch;
    }

    /**
     * Get batch by ID.
     */
    public Optional<Batch> getBatchById(String id) {
        return batchRepository.findById(id);
    }

    /**
     * Get all batches associated with a specific pond.
     */
    public List<Batch> getBatchesByPond(String pondId) {
        return batchRepository.findDistinctByFishSeedsPondId(pondId);
    }

    /**
     * Update batch details (name, dates).
     * Status is managed through fish seed status updates.
     */
    @Transactional
    public Optional<Batch> updateBatch(String id, Batch batchDetails) {
        return batchRepository.findById(id)
                .map(batch -> {
                    batch.setName(batchDetails.getName());
                    batch.setStockingDate(batchDetails.getStockingDate());
                    batch.setEstimatedHarvestDate(batchDetails.getEstimatedHarvestDate());

                    Batch updated = batchRepository.save(batch);
                    log.info("Updated batch: {}", id);
                    return updated;
                });
    }

    /**
     * Add a new fish seed to an existing batch.
     * Creates stock event and updates pond assignment.
     */
    @Transactional
    public Optional<Batch> addFishSeed(String batchId, FishSeed seed) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    String tenantId = batch.getTenantId();

                    // Ensure seed is treated as new
                    seed.setId(null);
                    seed.setTenantId(tenantId);
                    seed.setStatus("Active");

                    // Initialize collection if needed
                    if (batch.getFishSeeds() == null) {
                        batch.setFishSeeds(new ArrayList<>());
                    }

                    batch.getFishSeeds().add(seed);
                    Batch savedBatch = batchRepository.save(batch);

                    // Find the saved seed to get its generated ID
                    FishSeed savedSeed = savedBatch.getFishSeeds().stream()
                            .filter(s -> s.getFishTypeId().equals(seed.getFishTypeId())
                                    && s.getQuantity().equals(seed.getQuantity()))
                            .findFirst()
                            .orElse(seed);

                    // Create stock event
                    createStockEvent(savedBatch.getId(), savedSeed, FishStockEventType.SEED,
                            seed.getQuantity(), tenantId);

                    // Update pond assignment
                    updatePondBatchId(seed.getPondId(), savedBatch.getId());

                    log.info("Added fish seed to batch: {}, pond: {}", batchId, seed.getPondId());
                    return savedBatch;
                });
    }

    /**
     * Update status of a specific fish seed within a batch.
     */
    @Transactional
    public Optional<Batch> updateSeedStatus(String batchId, String seedId, String status) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    batch.getFishSeeds().stream()
                            .filter(s -> s.getId() != null && s.getId().equals(seedId))
                            .findFirst()
                            .ifPresent(s -> s.setStatus(status));

                    Batch updated = batchRepository.save(batch);
                    log.info("Updated seed status: {} in batch: {} to: {}", seedId, batchId, status);
                    return updated;
                });
    }

    /**
     * Update status for all fish seeds in a batch.
     */
    @Transactional
    public Optional<Batch> updateBatchStatus(String batchId, String status) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    if (batch.getFishSeeds() != null) {
                        batch.getFishSeeds().forEach(seed -> seed.setStatus(status));
                    }

                    Batch updated = batchRepository.save(batch);
                    log.info("Updated all seeds in batch: {} to status: {}", batchId, status);
                    return updated;
                });
    }

    /**
     * Move fish seed to a different pond.
     * Supports full or partial moves, creates transfer events.
     */
    @Transactional
    public Optional<Batch> moveFishSeed(String batchId, String seedId, String targetPondId, Integer quantity) {
        return batchRepository.findById(batchId)
                .map(batch -> {
                    FishSeed sourceSeed = batch.getFishSeeds().stream()
                            .filter(s -> s.getId() != null && s.getId().equals(seedId))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("Fish seed not found: " + seedId));

                    int moveQty = (quantity != null) ? quantity : sourceSeed.getQuantity();

                    if (moveQty > sourceSeed.getQuantity() || moveQty <= 0) {
                        throw new BusinessException("Invalid move quantity: " + moveQty);
                    }

                    String tenantId = TenantContext.getTenantId();
                    String originalPondId = sourceSeed.getPondId();

                    Batch savedBatch;
                    if (moveQty == sourceSeed.getQuantity()) {
                        // Full move - just update pond ID
                        sourceSeed.setPondId(targetPondId);
                        savedBatch = batchRepository.save(batch);
                    } else {
                        // Partial move - split the seed
                        sourceSeed.setQuantity(sourceSeed.getQuantity() - moveQty);

                        FishSeed newSeed = FishSeed.builder()
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
                        savedBatch = batchRepository.save(batch);
                    }

                    // Create transfer events
                    createTransferEvents(savedBatch.getId(), sourceSeed, originalPondId, targetPondId, moveQty, tenantId);

                    // Update target pond assignment
                    updatePondBatchId(targetPondId, savedBatch.getId());

                    log.info("Moved {} fish from seed {} to pond {}", moveQty, seedId, targetPondId);
                    return savedBatch;
                });
    }

    /**
     * Import batches from CSV file.
     * Creates batches, ponds, fish types, and seeds as needed.
     */
    @Transactional
    public List<Batch> importFromCsv(MultipartFile file) throws Exception {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            throw new BusinessException("Tenant context is required for import");
        }

        List<Batch> updatedBatches = new ArrayList<>();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<CsvFishSeed> csvToBean = new CsvToBeanBuilder<CsvFishSeed>(reader)
                    .withType(CsvFishSeed.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<CsvFishSeed> csvSeeds = csvToBean.parse();
            log.info("Importing {} fish seeds from CSV for tenant: {}", csvSeeds.size(), tenantId);

            for (CsvFishSeed csv : csvSeeds) {
                LocalDate eventDate = parseDate(csv.getDate());

                // Get or create batch
                Batch batch = batchRepository.findByName(csv.getBatchName())
                        .orElseGet(() -> createBatchFromCsv(csv.getBatchName(), eventDate, tenantId));

                // Get or create pond
                Pond pond = pondRepository.findByName(csv.getPondName())
                        .orElseGet(() -> createPondFromCsv(csv.getPondName(), tenantId));

                // Get or create fish type
                FishType fishType = fishTypeRepository.findByName(csv.getType())
                        .orElseGet(() -> createFishTypeFromCsv(csv.getType(), tenantId));

                // Create fish seed
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

                // Get persisted seed with ID
                FishSeed savedSeed = savedBatch.getFishSeeds().stream()
                        .filter(s -> s.getFishTypeId().equals(seed.getFishTypeId())
                                && s.getQuantity().equals(seed.getQuantity())
                                && s.getDate().equals(seed.getDate()))
                        .findFirst()
                        .orElse(seed);

                // Create stock event
                createStockEvent(savedBatch.getId(), savedSeed, FishStockEventType.SEED,
                        seed.getQuantity(), tenantId);

                // Update pond assignment
                pond.setCurrentBatchId(savedBatch.getId());
                pondRepository.save(pond);

                // Track updated batches
                if (updatedBatches.stream().noneMatch(b -> b.getId().equals(savedBatch.getId()))) {
                    updatedBatches.add(savedBatch);
                }
            }
        }

        log.info("Successfully imported {} batches from CSV", updatedBatches.size());
        return updatedBatches;
    }

    // ========== Private Helper Methods ==========

    private void createStockEvent(String batchId, FishSeed seed, FishStockEventType type,
                                   Integer quantityDelta, String tenantId) {
        FishStockEvent event = FishStockEvent.builder()
                .pondId(seed.getPondId())
                .batchId(batchId)
                .date(seed.getDate())
                .type(type)
                .fishTypeId(seed.getFishTypeId())
                .quantityDelta(quantityDelta)
                .avgWeightGram(seed.getSizeGrams())
                .referenceId(seed.getId())
                .tenantId(tenantId)
                .build();

        fishStockEventRepository.save(event);
        log.debug("Created stock event: {} for batch: {}", type, batchId);
    }

    private void createTransferEvents(String batchId, FishSeed seed, String sourcePondId,
                                       String targetPondId, int quantity, String tenantId) {
        // Transfer out of source pond
        fishStockEventRepository.save(FishStockEvent.builder()
                .pondId(sourcePondId)
                .batchId(batchId)
                .date(LocalDate.now())
                .type(FishStockEventType.TRANSFER_OUT)
                .fishTypeId(seed.getFishTypeId())
                .quantityDelta(-quantity)
                .avgWeightGram(seed.getSizeGrams())
                .referenceId(seed.getId())
                .tenantId(tenantId)
                .build());

        // Transfer into target pond
        fishStockEventRepository.save(FishStockEvent.builder()
                .pondId(targetPondId)
                .batchId(batchId)
                .date(LocalDate.now())
                .type(FishStockEventType.TRANSFER_IN)
                .fishTypeId(seed.getFishTypeId())
                .quantityDelta(quantity)
                .avgWeightGram(seed.getSizeGrams())
                .referenceId(seed.getId())
                .tenantId(tenantId)
                .build());

        log.debug("Created transfer events: {} -> {}", sourcePondId, targetPondId);
    }

    private void updatePondBatchAssignment(Batch batch) {
        if (batch.getFishSeeds() != null && !batch.getFishSeeds().isEmpty()) {
            String pondId = batch.getFishSeeds().get(0).getPondId();
            updatePondBatchId(pondId, batch.getId());
        }
    }

    private void updatePondBatchId(String pondId, String batchId) {
        if (pondId != null) {
            pondRepository.findById(pondId).ifPresent(pond -> {
                pond.setCurrentBatchId(batchId);
                pondRepository.save(pond);
                log.debug("Updated pond {} currentBatchId to {}", pondId, batchId);
            });
        }
    }

    private Batch createBatchFromCsv(String name, LocalDate stockingDate, String tenantId) {
        Batch batch = Batch.builder()
                .name(name)
                .tenantId(tenantId)
                .stockingDate(stockingDate)
                .build();
        return batchRepository.save(batch);
    }

    private Pond createPondFromCsv(String name, String tenantId) {
        Pond pond = Pond.builder()
                .name(name)
                .tenantId(tenantId)
                .status("Active")
                .build();
        return pondRepository.save(pond);
    }

    private FishType createFishTypeFromCsv(String name, String tenantId) {
        FishType fishType = FishType.builder()
                .name(name)
                .tenantId(tenantId)
                .build();
        return fishTypeRepository.save(fishType);
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
                // Try next format
            }
        }

        log.warn("Could not parse date: {}, using current date", dateStr);
        return LocalDate.now();
    }
}
