package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantAuthorizationService;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed-stock")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class FeedStockController {

    private final FeedStockRepository feedStockRepository;
    private final TenantAuthorizationService tenantAuthorizationService;

    @GetMapping
    public List<FeedStock> getAllFeedStock() {
        String effectiveTenantId = TenantContext.getEffectiveFeedTenantId();
        return feedStockRepository.findByTenantId(effectiveTenantId);
    }

    @GetMapping("/low")
    public List<FeedStock> getLowStock() {
        String effectiveTenantId = TenantContext.getEffectiveFeedTenantId();
        return feedStockRepository.findLowStockByTenantId(effectiveTenantId);
    }

    @PostMapping
    public FeedStock createFeedStock(@RequestBody FeedStock feedStock) {
        tenantAuthorizationService.requireRootTenant();
        feedStock.setTenantId(TenantContext.getTenantId());
        return feedStockRepository.save(feedStock);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedStock> updateFeedStock(
            @PathVariable String id,
            @RequestBody FeedStock feedStockDetails) {
        tenantAuthorizationService.requireRootTenant();
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
        tenantAuthorizationService.requireRootTenant();
        return feedStockRepository.findById(id)
                .map(feedStock -> {
                    feedStockRepository.delete(feedStock);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
