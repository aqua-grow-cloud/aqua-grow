package com.aquagrow.api.controller;

import com.aquagrow.api.model.FishStockEvent;
import com.aquagrow.api.model.FishStockEventType;
import com.aquagrow.api.service.FishStockEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for fish stock events.
 * Delegates all business logic to FishStockEventService.
 */
@RestController
@RequestMapping("/api/fish-stock-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class FishStockEventController {

    private final FishStockEventService fishStockEventService;

    @GetMapping
    public List<FishStockEvent> getAllEvents() {
        return fishStockEventService.getAllStockEvents();
    }

    @PostMapping
    public FishStockEvent createEvent(@RequestBody FishStockEvent event) {
        return fishStockEventService.createStockEvent(event);
    }

    @GetMapping("/batch/{batchId}")
    public List<FishStockEvent> getEventsByBatch(@PathVariable String batchId) {
        return fishStockEventService.getStockEventsByBatch(batchId);
    }

    @GetMapping("/pond/{pondId}")
    public List<FishStockEvent> getEventsByPond(@PathVariable String pondId) {
        return fishStockEventService.getStockEventsByPond(pondId);
    }

    @GetMapping("/batch/{batchId}/current-stock")
    public Integer getCurrentStock(@PathVariable String batchId) {
        return fishStockEventService.calculateCurrentStock(batchId);
    }

    @GetMapping("/pond/{pondId}/current-stock")
    public Integer getPondStock(@PathVariable String pondId) {
        return fishStockEventService.calculatePondStock(pondId);
    }

    @GetMapping("/batch/{batchId}/breakdown")
    public Map<FishStockEventType, Integer> getStockBreakdown(@PathVariable String batchId) {
        return fishStockEventService.calculateStockBreakdown(batchId);
    }

    @GetMapping("/batch/{batchId}/biomass")
    public Double getEstimatedBiomass(@PathVariable String batchId) {
        return fishStockEventService.calculateEstimatedBiomass(batchId);
    }
}
