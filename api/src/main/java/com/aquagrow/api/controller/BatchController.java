package com.aquagrow.api.controller;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishSeed;
import com.aquagrow.api.service.BatchService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for Batch operations.
 * Delegates all business logic to BatchService.
 */
@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    public List<Batch> getAllBatches(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String pondId,
            @RequestParam(required = false) String fishTypeId) {
        return batchService.getAllBatches(name, pondId, fishTypeId);
    }

    @PostMapping
    public Batch createBatch(@RequestBody Batch batch) {
        return batchService.createBatch(batch);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Batch> getBatchById(@PathVariable String id) {
        return batchService.getBatchById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pond/{pondId}")
    public List<Batch> getBatchesByPond(@PathVariable String pondId) {
        return batchService.getBatchesByPond(pondId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Batch> updateBatch(@PathVariable String id, @RequestBody Batch batchDetails) {
        return batchService.updateBatch(id, batchDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/seeds")
    public ResponseEntity<Batch> addFishSeed(@PathVariable String id, @RequestBody FishSeed seed) {
        return batchService.addFishSeed(id, seed)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/seeds/{seedId}/status")
    public ResponseEntity<Batch> updateSeedStatus(
            @PathVariable String id,
            @PathVariable String seedId,
            @RequestParam String status) {
        return batchService.updateSeedStatus(id, seedId, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Batch> updateBatchStatus(@PathVariable String id, @RequestParam String status) {
        return batchService.updateBatchStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/seeds/{seedId}/move")
    public ResponseEntity<Batch> moveFishSeed(
            @PathVariable String id,
            @PathVariable String seedId,
            @RequestParam String targetPondId,
            @RequestParam(required = false) Integer quantity) {
        return batchService.moveFishSeed(id, seedId, targetPondId, quantity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/seeds/{seedId}/mortality")
    public ResponseEntity<Batch> logMortality(
            @PathVariable String id,
            @PathVariable String seedId,
            @RequestParam int quantity,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String note) {
        LocalDate eventDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
        return batchService.logMortality(id, seedId, quantity, eventDate, note)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/import")
    public List<Batch> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        return batchService.importFromCsv(file);
    }
}
