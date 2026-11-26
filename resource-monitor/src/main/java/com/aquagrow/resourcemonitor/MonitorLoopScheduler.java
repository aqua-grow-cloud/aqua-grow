package com.aquagrow.resourcemonitor;

import com.aquagrow.resourcemonitor.config.MonitorProperties;
import com.aquagrow.resourcemonitor.k8s.PodMetricsClient;
import com.aquagrow.resourcemonitor.logging.ResourceLogger;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import com.aquagrow.resourcemonitor.rules.AnomalyClassifier;
import com.aquagrow.resourcemonitor.rules.AnomalyResult;
import com.aquagrow.resourcemonitor.rules.CycleState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MonitorLoopScheduler {

    private final PodMetricsClient podMetricsClient;
    private final AnomalyClassifier classifier;
    private final CycleState cycleState;
    private final ResourceLogger resourceLogger;
    private final MonitorProperties properties;
    private final ObjectMapper mapper = new ObjectMapper();

    public MonitorLoopScheduler(PodMetricsClient podMetricsClient,
                                 AnomalyClassifier classifier,
                                 CycleState cycleState,
                                 ResourceLogger resourceLogger,
                                 MonitorProperties properties) {
        this.podMetricsClient = podMetricsClient;
        this.classifier = classifier;
        this.cycleState = cycleState;
        this.resourceLogger = resourceLogger;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${monitor.poll-interval-seconds:60}000")
    public void pollCycle() {
        runOnce();
    }

    /**
     * One full poll cycle. Never throws — any failure is logged as a degraded-agent
     * status line so the poll loop itself keeps running indefinitely.
     */
    public void runOnce() {
        try {
            Map<String, List<PodSnapshot>> snapshotsBySelector = podMetricsClient.fetchSnapshotsBySelector(
                    properties.getNamespace(), properties.getSelectors());

            List<PodSnapshot> snapshots = new ArrayList<>();
            for (Map.Entry<String, List<PodSnapshot>> entry : snapshotsBySelector.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    log.info(noPodsMatchedSelectorJson(entry.getKey()));
                } else {
                    snapshots.addAll(entry.getValue());
                }
            }

            for (PodSnapshot snapshot : snapshots) {
                AnomalyResult result = classifier.classify(snapshot, cycleState, properties.getThresholds());
                resourceLogger.log(snapshot, result);
            }
        } catch (Exception e) {
            log.warn(degradedAgentStatusJson(e.getMessage()));
        }
    }

    @SneakyThrows
    private String noPodsMatchedSelectorJson(String selector) {
        ObjectNode node = mapper.createObjectNode();
        node.put("agent_status", "no pods matched selector");
        node.put("selector", selector);
        return mapper.writeValueAsString(node);
    }

    @SneakyThrows
    private String degradedAgentStatusJson(String errorMessage) {
        ObjectNode node = mapper.createObjectNode();
        node.put("agent_status", "degraded");
        node.put("error", errorMessage);
        return mapper.writeValueAsString(node);
    }
}
