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

    @GetMapping
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    @PostMapping
    public Batch createBatch(@RequestBody Batch batch) {
        return batchRepository.save(batch);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Batch> getBatchById(@PathVariable String id) {
        return batchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pond/{pondId}")
    public List<Batch> getBatchesByPond(@PathVariable String pondId) {
        return batchRepository.findByPondId(pondId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Batch> updateBatch(@PathVariable String id, @RequestBody Batch batchDetails) {
        return batchRepository.findById(id)
                .map(batch -> {
                    batch.setName(batchDetails.getName());
                    batch.setStockingDate(batchDetails.getStockingDate());
                    batch.setEstimatedHarvestDate(batchDetails.getEstimatedHarvestDate());
                    batch.setSpecies(batchDetails.getSpecies());
                    batch.setInitialQuantity(batchDetails.getInitialQuantity());
                    batch.setSeedSize(batchDetails.getSeedSize());
                    batch.setStatus(batchDetails.getStatus());

                    if (batchDetails.getFishSeeds() != null) {
                        batch.getFishSeeds().clear();
                        if (!batchDetails.getFishSeeds().isEmpty()) {
                            batch.getFishSeeds().addAll(batchDetails.getFishSeeds());
                        }
                    }

                    return ResponseEntity.ok(batchRepository.save(batch));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
