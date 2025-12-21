package com.aquagrow.api.controller;

import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.repository.FeedLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeedLogController {

    private final FeedLogRepository feedLogRepository;

    @GetMapping
    public List<FeedLog> getAllFeedLogs() {
        return feedLogRepository.findAll();
    }

    @PostMapping
    public FeedLog logFeed(@RequestBody FeedLog log) {
        return feedLogRepository.save(log);
    }

    @GetMapping("/batch/{batchId}")
    public List<FeedLog> getFeedLogsByBatch(@PathVariable String batchId) {
        return feedLogRepository.findByBatchId(batchId);
    }
}
