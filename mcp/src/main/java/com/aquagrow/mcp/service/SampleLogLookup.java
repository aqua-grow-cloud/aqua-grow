package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.SampleLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Looks up a batch's earlier samples for the sampling tools (same-day duplicates and the
 * previous average weight).
 */
@Component
@RequiredArgsConstructor
public class SampleLogLookup {

    public record Existing(String date, Integer sampleCount, Double averageWeight) {
    }

    /** Samples already on the same day, plus the latest earlier sample with a weight. */
    public record Context(List<Existing> sameDay, Existing previous) {
    }

    private final FarmDataClient farmData;

    public Context context(String batchId, String fishTypeId, LocalDate date) {
        List<SampleLog> sameType = farmData.sampleLogsByBatch(batchId).stream()
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
