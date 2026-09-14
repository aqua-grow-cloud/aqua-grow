package com.aquagrow.resourcemonitor.logging;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ResourceLogEntry {
    private final Instant timestamp;
    private final String pod;
    private final String container;
    private final Long cpuUsagePctOfLimit;
    private final Long memUsagePctOfLimit;
    private final int restartCount;
    private final boolean ready;
    private final String phase;
    private final List<String> recentEvents;
    private final String level;
    private final List<String> reasons;
}
