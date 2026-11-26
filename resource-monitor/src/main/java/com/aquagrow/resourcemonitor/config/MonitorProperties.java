package com.aquagrow.resourcemonitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {

    private String namespace;
    private int pollIntervalSeconds;
    private List<String> selectors;

    @NestedConfigurationProperty
    private Thresholds thresholds = new Thresholds();

    @Data
    public static class Thresholds {
        private int memWarnPct;
        private int memAlertPct;
        private int cpuWarnPct;
        private int cpuWarnConsecutiveCycles;
        private int notReadyAlertConsecutiveCycles;
    }
}
