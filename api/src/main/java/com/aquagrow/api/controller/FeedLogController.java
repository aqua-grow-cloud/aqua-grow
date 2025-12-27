package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeedLogController {

    private final FeedLogRepository feedLogRepository;
    private final FeedStockRepository feedStockRepository;

    private String getTenantId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("tenant_id");
    }

    @GetMapping
    public List<FeedLog> getAllFeedLogs() {
        return feedLogRepository.findAll();
    }

    @PostMapping
    public FeedLog logFeed(@RequestBody FeedLog log, Authentication authentication) {
        String tenantId = getTenantId(authentication);

        // Deduct from inventory
        feedStockRepository.findByTenantIdAndFeedType(tenantId, log.getFeedType())
                .ifPresent(stock -> {
                    stock.setCurrentStock(stock.getCurrentStock() - log.getAmount());
                    feedStockRepository.save(stock);
                });

        return feedLogRepository.save(log);
    }

    @GetMapping("/batch/{batchId}")
    public List<FeedLog> getFeedLogsByBatch(@PathVariable String batchId) {
        return feedLogRepository.findByBatchId(batchId);
    }
}
