package com.aquagrow.api.controller;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BatchController {

    private final BatchRepository batchRepository;
    private final com.aquagrow.api.repository.PondRepository pondRepository;

    @GetMapping
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    @PostMapping
    public Batch createBatch(@RequestBody Batch batch) {
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
                    batch.setStatus(batchDetails.getStatus());
                    batch.getFishSeeds().addAll(batchDetails.getFishSeeds());
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
}
