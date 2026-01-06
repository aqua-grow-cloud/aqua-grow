package com.aquagrow.api.controller;

import com.aquagrow.api.model.FishStockEvent;
import com.aquagrow.api.repository.FishStockEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fish-stock-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FishStockEventController {

    private final FishStockEventRepository fishStockEventRepository;

    @GetMapping
    public List<FishStockEvent> getAllEvents() {
        return fishStockEventRepository.findAll();
    }

    @GetMapping("/batch/{batchId}")
    public List<FishStockEvent> getEventsByBatch(@PathVariable String batchId) {
        return fishStockEventRepository.findByBatchId(batchId);
    }

    @GetMapping("/pond/{pondId}")
    public List<FishStockEvent> getEventsByPond(@PathVariable String pondId) {
        return fishStockEventRepository.findByPondId(pondId);
    }
}
