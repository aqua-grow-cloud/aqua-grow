package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.repository.FeedLogRepository;
import com.aquagrow.mcp.service.FeedingWriteMcpService.Conflict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Short read-only database lookups for the feeding tools, kept in their own
 * transactional bean so the tools themselves never hold a database
 * transaction open while they call aqua-grow-api over HTTP.
 */
@Component
@RequiredArgsConstructor
public class FeedLogLookup {

    private final FeedLogRepository feedLogRepository;
    private final TenantFilterActivator tenantFilterActivator;

    /** Existing feed logs for the batch (any feed type) dated within [from, to], oldest first. */
    @Transactional(readOnly = true)
    public List<Conflict> conflicts(String batchId, LocalDate from, LocalDate to) {
        String tenantId = tenantFilterActivator.activate();
        return feedLogRepository.findByTenantIdAndBatchId(tenantId, batchId).stream()
                .filter(l -> l.getDate() != null && !l.getDate().isBefore(from) && !l.getDate().isAfter(to))
                .sorted(Comparator.comparing(FeedLog::getDate))
                .map(l -> new Conflict(l.getDate().toString(), l.getFeedType(), l.getAmount()))
                .toList();
    }
}
