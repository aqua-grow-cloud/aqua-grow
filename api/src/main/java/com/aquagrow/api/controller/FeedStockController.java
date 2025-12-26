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

    private String getTenantId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("tenant_id");
    }

    @GetMapping
    public List<FeedStock> getAllFeedStock(Authentication authentication) {
        String tenantId = getTenantId(authentication);
        return feedStockRepository.findByTenantId(tenantId);
    }

    @GetMapping("/low")
    public List<FeedStock> getLowStock(Authentication authentication) {
        String tenantId = getTenantId(authentication);
        return feedStockRepository.findLowStockByTenantId(tenantId);
    }

    @PostMapping
    public FeedStock createFeedStock(@RequestBody FeedStock feedStock, Authentication authentication) {
        String tenantId = getTenantId(authentication);
        feedStock.setTenantId(tenantId);
        return feedStockRepository.save(feedStock);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeedStock> updateFeedStock(
            @PathVariable String id,
            @RequestBody FeedStock feedStockDetails,
            Authentication authentication) {
        String tenantId = getTenantId(authentication);

        return feedStockRepository.findById(id)
                .filter(feedStock -> feedStock.getTenantId().equals(tenantId))
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
    public ResponseEntity<Void> deleteFeedStock(@PathVariable String id, Authentication authentication) {
        String tenantId = getTenantId(authentication);

        return feedStockRepository.findById(id)
                .filter(feedStock -> feedStock.getTenantId().equals(tenantId))
                .map(feedStock -> {
                    feedStockRepository.delete(feedStock);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
