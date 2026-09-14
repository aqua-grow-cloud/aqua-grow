package com.aquagrow.resourcemonitor.model;

import java.util.List;

public record PodSnapshot(
        String podName,
        String containerName,
        Long cpuUsageMillicores,
        Long memUsageBytes,
        Long cpuLimitMillicores,
        Long memLimitBytes,
        int restartCount,
        boolean ready,
        String phase,
        List<String> recentEventReasons
) {
}
