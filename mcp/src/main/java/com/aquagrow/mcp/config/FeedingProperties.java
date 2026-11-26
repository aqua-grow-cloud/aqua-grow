package com.aquagrow.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Limits and defaults for feeding writes. */
@Component
public class FeedingProperties {

    private final double defaultBagSizeKg;
    private final double maxKgPerDay;
    private final int maxDays;

    public FeedingProperties(
            @Value("${aquagrow.mcp.conventions.default-bag-size-kg:40}") double defaultBagSizeKg,
            @Value("${aquagrow.mcp.feeding.max-kg-per-day:5000}") double maxKgPerDay,
            @Value("${aquagrow.mcp.feeding.max-days:62}") int maxDays) {
        this.defaultBagSizeKg = defaultBagSizeKg;
        this.maxKgPerDay = maxKgPerDay;
        this.maxDays = maxDays;
    }

    public double defaultBagSizeKg() {
        return defaultBagSizeKg;
    }

    public double maxKgPerDay() {
        return maxKgPerDay;
    }

    public int maxDays() {
        return maxDays;
    }
}
