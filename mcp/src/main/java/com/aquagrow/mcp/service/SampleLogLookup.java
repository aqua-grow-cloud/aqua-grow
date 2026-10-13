package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.SampleLog;
import com.aquagrow.mcp.repository.SampleLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Short read-only lookups of past samples for the sampling tools, in their own
 * transactional bean so the tools never hold a database transaction open while
 * they call aqua-grow-api over HTTP.
 */
@Component
@RequiredArgsConstructor
public class SampleLogLookup {

    public record Existing(String date, Integer sampleCount, Double averageWeight) {
    }

    /** Samples already on the same day, plus the latest earlier sample with a weight. */
    public record Context(List<Existing> sameDay, Existing previous) {
    }

    private final SampleLogRepository sampleLogRepository;
    private final TenantFilterActivator tenantFilterActivator;

    @Transactional(readOnly = true)
    public Context context(String batchId, String fishTypeId, LocalDate date) {
        String tenantId = tenantFilterActivator.activate();
        List<SampleLog> sameType = sampleLogRepository.findByTenantIdAndBatchId(tenantId, batchId).stream()
                .filter(s -> s.getDate() != null && Objects.equals(fishTypeId, s.getFishTypeId()))
                .toList();

        List<Existing> sameDay = sameType.stream()
                .filter(s -> s.getDate().equals(date))
                .map(SampleLogLookup::toExisting)
                .toList();
        Existing previous = sameType.stream()
                .filter(s -> s.getDate().isBefore(date) && s.getAverageWeight() != null && s.getAverageWeight() > 0)
                .max(Comparator.comparing(SampleLog::getDate))
                .map(SampleLogLookup::toExisting)
                .orElse(null);
        return new Context(sameDay, previous);
    }

    private static Existing toExisting(SampleLog s) {
        return new Existing(s.getDate().toString(), s.getSampleCount(), s.getAverageWeight());
    }
}
