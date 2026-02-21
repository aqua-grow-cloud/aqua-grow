package com.aquagrow.api.controller;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishType;
import com.aquagrow.api.service.FishTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fish-types")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class FishTypeController {

    private final FishTypeService fishTypeService;

    @GetMapping
    public List<FishType> getAllFishTypes() {
        return fishTypeService.getAllFishTypes();
    }

    @GetMapping("/{id}")
    public FishType getFishTypeById(@PathVariable String id) {
        return fishTypeService.getFishTypeById(id)
                .orElseThrow(() -> new com.aquagrow.api.exception.BusinessException("Fish type not found: " + id));
    }

    @PostMapping
    public FishType createFishType(@RequestBody FishType fishType) {
        return fishTypeService.createFishType(fishType);
    }

    @PutMapping("/{id}")
    public FishType updateFishType(@PathVariable String id, @RequestBody FishType updates) {
        return fishTypeService.updateFishType(id, updates);
    }

    @DeleteMapping("/{id}")
    public void deleteFishType(@PathVariable String id) {
        fishTypeService.deleteFishType(id);
    }

    // ==================== Analytics Endpoints ====================

    @GetMapping("/search")
    public List<FishType> searchFishTypes(@RequestParam String query) {
        return fishTypeService.searchByName(query);
    }

    @GetMapping("/{id}/in-use")
    public boolean isInUse(@PathVariable String id) {
        return fishTypeService.isInUse(id);
    }

    @GetMapping("/{id}/batch-count")
    public long getBatchCount(@PathVariable String id) {
        return fishTypeService.getBatchCountForFishType(id);
    }

    @GetMapping("/{id}/batches")
    public List<Batch> getBatches(@PathVariable String id) {
        return fishTypeService.getBatchesForFishType(id);
    }
}
