package com.aquagrow.resourcemonitor.logging;

import com.aquagrow.resourcemonitor.model.PodSnapshot;
import com.aquagrow.resourcemonitor.rules.AnomalyResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class ResourceLogger {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @SneakyThrows
    public String toJsonLine(PodSnapshot snapshot, AnomalyResult result) {
        ObjectNode node = mapper.createObjectNode();
        node.put("timestamp", Instant.now().toString());
        node.put("pod", snapshot.podName());
        node.put("container", snapshot.containerName());
        putPercentIfPresent(node, "cpu_usage_pct_of_limit", snapshot.cpuUsageMillicores(), snapshot.cpuLimitMillicores());
        putPercentIfPresent(node, "mem_usage_pct_of_limit", snapshot.memUsageBytes(), snapshot.memLimitBytes());
        node.put("restart_count", snapshot.restartCount());
        node.put("ready", snapshot.ready());
        node.put("phase", snapshot.phase());
        node.putPOJO("recent_events", snapshot.recentEventReasons());
        node.put("level", result.level().name());
        if (!result.reasons().isEmpty()) {
            node.putPOJO("reasons", result.reasons());
        }
        return mapper.writeValueAsString(node);
    }

    private void putPercentIfPresent(ObjectNode node, String field, Long usage, Long limit) {
        if (usage != null && limit != null && limit > 0) {
            long pct = Math.round((usage * 100.0) / limit);
            node.put(field, pct);
        } else {
            node.putNull(field);
        }
    }

    public void log(PodSnapshot snapshot, AnomalyResult result) {
        log.info(toJsonLine(snapshot, result));
    }
}
