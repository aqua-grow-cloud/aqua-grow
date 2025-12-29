package com.aquagrow.api.controller;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Join;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BatchController {

    private final BatchRepository batchRepository;
    private final com.aquagrow.api.repository.PondRepository pondRepository;

    @GetMapping
    public List<Batch> getAllBatches(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String pondId,
            @RequestParam(required = false) String fishType) {

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

            if (fishType != null && !fishType.isEmpty()) {
                Join<Object, Object> seeds = root.join("fishSeeds");
                predicates.add(criteriaBuilder.equal(seeds.get("type"), fishType));
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        });
    }

    @PostMapping
    public Batch createBatch(@RequestBody Batch batch) {
        // Tenant ID is set automatically by listeners or not needed on input if aspect
        // handles view
        // But for creation, we SHOULD set it.
        // Actually, the Filter enforces VIEWING, but for CREATION we still need to set
        // the tenantId.
        // We can do this via an EntityListener or keep a small helper.
        // For now, let's use TenantContext to set it.
        batch.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        Batch savedBatch = batchRepository.save(batch);
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
                    // Ensure the seed is treated as new
                    seed.setId(null);
                    // Inherit tenantId from Batch
                    seed.setTenantId(batch.getTenantId());
                    seed.setStatus("Active"); // Default to Active

                    // Ensure the collection is initialized
                    if (batch.getFishSeeds() == null) {
                        batch.setFishSeeds(new java.util.ArrayList<>());
                    }
                    batch.getFishSeeds().add(seed);
                    Batch savedBatch = batchRepository.save(batch);
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
}
