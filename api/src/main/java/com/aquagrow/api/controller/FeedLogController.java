package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for feed logs and feed consumption analytics.
 * Delegates all business logic to FeedService.
 */
@RestController
@RequestMapping("/api/feed-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class FeedLogController {

    private final FeedService feedService;

    @GetMapping
    public List<FeedLog> getAllFeedLogs() {
        return feedService.getAllFeedLogs();
    }

    @PostMapping
    public FeedLog logFeed(@RequestBody FeedLog feedLog) {
        return feedService.createFeedLog(feedLog);
    }

    @GetMapping("/batch/{batchId}")
    public List<FeedLog> getFeedLogsByBatch(@PathVariable String batchId) {
        return feedService.getFeedLogsByBatch(batchId);
    }

    @GetMapping("/batch/{batchId}/total")
    public Double getTotalFeedConsumed(@PathVariable String batchId) {
        return feedService.calculateTotalFeedConsumed(batchId);
    }

    @GetMapping("/batch/{batchId}/total-range")
    public Double getFeedConsumedInDateRange(
            @PathVariable String batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return feedService.calculateFeedConsumedInDateRange(batchId, startDate, endDate);
    }

    @GetMapping("/batch/{batchId}/breakdown")
    public Map<String, Double> getFeedBreakdownByType(@PathVariable String batchId) {
        return feedService.calculateFeedBreakdownByType(batchId);
    }

    @GetMapping("/batch/{batchId}/average-daily")
    public Double getAverageDailyFeed(@PathVariable String batchId) {
        return feedService.calculateAverageDailyFeed(batchId);
    }

    @GetMapping("/batch/{batchId}/fcr")
    public Double calculateFCR(
            @PathVariable String batchId,
            @RequestParam Double totalFeedKg,
            @RequestParam Double initialBiomassKg,
            @RequestParam Double currentBiomassKg) {
        return feedService.calculateFCR(batchId, totalFeedKg, initialBiomassKg, currentBiomassKg);
    }

    @GetMapping("/low-stock-alerts")
    public List<FeedStock> getLowStockAlerts() {
        return feedService.getLowStockAlerts();
    }
}
