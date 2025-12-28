package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed-stock")
@CrossOrigin(origins = "*")
public class FeedStockController {

    @Autowired
    private FeedStockRepository feedStockRepository;

    @GetMapping
    public List<FeedStock> getAllFeedStock() {
        return feedStockRepository.findAll();
    }

    @GetMapping("/low")
    public List<FeedStock> getLowStock() {
        return feedStockRepository.findByCurrentStockLessThanThreshold();
    }

    @PostMapping
    public FeedStock createFeedStock(@RequestBody FeedStock feedStock) {
        feedStock.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        return feedStockRepository.save(feedStock);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedStock> updateFeedStock(
            @PathVariable String id,
            @RequestBody FeedStock feedStockDetails) {
        return feedStockRepository.findById(id)
                .map(feedStock -> {
                    feedStock.setFeedType(feedStockDetails.getFeedType());
                    feedStock.setCurrentStock(feedStockDetails.getCurrentStock());
                    feedStock.setThreshold(feedStockDetails.getThreshold());
                    feedStock.setUnit(feedStockDetails.getUnit());
                    feedStock.setSupplierId(feedStockDetails.getSupplierId());
                    feedStock.setLastStockingDate(feedStockDetails.getLastStockingDate());
                    return ResponseEntity.ok(feedStockRepository.save(feedStock));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedStock(@PathVariable String id) {
        return feedStockRepository.findById(id)
                .map(feedStock -> {
                    feedStockRepository.delete(feedStock);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
