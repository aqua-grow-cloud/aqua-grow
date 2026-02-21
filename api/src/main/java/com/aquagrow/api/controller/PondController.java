package com.aquagrow.api.controller;

import com.aquagrow.api.model.Pond;
import com.aquagrow.api.service.PondService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for pond management and analytics.
 * Delegates all business logic to PondService.
 */
@RestController
@RequestMapping("/api/ponds")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class PondController {

    private final PondService pondService;

    @GetMapping
    public List<Pond> getAllPonds() {
        return pondService.getAllPonds();
    }

    @PostMapping
    public Pond createPond(@RequestBody Pond pond) {
        return pondService.createPond(pond);
    }

    @GetMapping("/{id}")
    public Pond getPondById(@PathVariable String id) {
        return pondService.getPondById(id);
    }

    @PutMapping("/{id}")
    public Pond updatePond(@PathVariable String id, @RequestBody Pond pondDetails) {
        return pondService.updatePond(id, pondDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePond(@PathVariable String id) {
        pondService.deletePond(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/volume")
    public Double getWaterVolume(@PathVariable String id) {
        return pondService.calculateWaterVolume(id);
    }

    @GetMapping("/{id}/max-capacity")
    public Integer getMaxStockingCapacity(@PathVariable String id) {
        return pondService.calculateMaxStockingCapacity(id);
    }

    @GetMapping("/{id}/stocking-density")
    public Double getCurrentStockingDensity(
            @PathVariable String id,
            @RequestParam Integer currentStock) {
        return pondService.calculateCurrentStockingDensity(id, currentStock);
    }

    @GetMapping("/{id}/can-accommodate")
    public Boolean canAccommodateFish(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "0") Integer currentStock,
            @RequestParam Integer additionalFish) {
        return pondService.canAccommodateFish(id, currentStock, additionalFish);
    }

    @GetMapping("/status/{status}")
    public List<Pond> getPondsByStatus(@PathVariable String status) {
        return pondService.getPondsByStatus(status);
    }

    @GetMapping("/active")
    public List<Pond> getActivePonds() {
        return pondService.getActivePonds();
    }

    @GetMapping("/available")
    public List<Pond> getAvailablePonds() {
        return pondService.getAvailablePonds();
    }

    @GetMapping("/total-area")
    public Double getTotalPondArea() {
        return pondService.calculateTotalPondArea();
    }

    @GetMapping("/utilization")
    public Double getPondUtilization() {
        return pondService.calculatePondUtilization();
    }
}
