package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantAuthorizationService;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    @GetMapping
    public List<FeedStock> getAllFeedStock() {
        String effectiveTenantId = TenantContext.getEffectiveFeedTenantId();
        scopeTenantFilterTo(effectiveTenantId);
        return feedStockRepository.findByTenantId(effectiveTenantId);
    }

    @GetMapping("/low")
    public List<FeedStock> getLowStock() {
        String effectiveTenantId = TenantContext.getEffectiveFeedTenantId();
        scopeTenantFilterTo(effectiveTenantId);
        return feedStockRepository.findLowStockByTenantId(effectiveTenantId);
    }

    /**
     * Re-scopes the Hibernate {@code tenantFilter} to the effective feed tenant id.
     * TenantAspect enables the filter pinned to the current (possibly sub-) tenant on
     * every controller call; for feed-stock reads a sub-tenant must see its root/parent
     * tenant's stock, so we override the filter parameter to the effective tenant id.
     * This keeps defense-in-depth filtering active while scoped to the correct tenant.
     */
    private void scopeTenantFilterTo(String effectiveTenantId) {
        entityManager.unwrap(org.hibernate.Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", effectiveTenantId);
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
                .filter(fs -> TenantContext.getTenantId().equals(fs.getTenantId()))
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
                .filter(fs -> TenantContext.getTenantId().equals(fs.getTenantId()))
                .map(feedStock -> {
                    feedStockRepository.delete(feedStock);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
