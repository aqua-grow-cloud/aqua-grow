package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.service.FeedingWriteMcpService.Conflict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Finds feed logs that already exist for a batch, so a new feeding is not double counted. */
@Component
@RequiredArgsConstructor
public class FeedLogLookup {

    private final FarmDataClient farmData;

    /** Existing feed logs for the batch (any feed type) dated within [from, to], oldest first. */
    public List<Conflict> conflicts(String batchId, LocalDate from, LocalDate to) {
        return farmData.feedLogsByBatch(batchId).stream()
                .filter(l -> l.getDate() != null && !l.getDate().isBefore(from) && !l.getDate().isAfter(to))
                .sorted(Comparator.comparing(FeedLog::getDate))
                .map(l -> new Conflict(l.getDate().toString(), l.getFeedType(), l.getAmount()))
                .toList();
    }
}
