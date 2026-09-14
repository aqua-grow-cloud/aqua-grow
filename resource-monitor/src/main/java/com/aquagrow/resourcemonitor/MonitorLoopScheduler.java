package com.aquagrow.resourcemonitor;

import com.aquagrow.resourcemonitor.config.MonitorProperties;
import com.aquagrow.resourcemonitor.k8s.PodMetricsClient;
import com.aquagrow.resourcemonitor.logging.ResourceLogger;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import com.aquagrow.resourcemonitor.rules.AnomalyClassifier;
import com.aquagrow.resourcemonitor.rules.AnomalyResult;
import com.aquagrow.resourcemonitor.rules.CycleState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class MonitorLoopScheduler {

    private final PodMetricsClient podMetricsClient;
    private final AnomalyClassifier classifier;
    private final CycleState cycleState;
    private final ResourceLogger resourceLogger;
    private final MonitorProperties properties;

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
            List<PodSnapshot> snapshots = podMetricsClient.fetchSnapshots(
                    properties.getNamespace(), properties.getSelectors());

            if (snapshots.isEmpty()) {
                log.info("{{\"agent_status\":\"no pods matched selectors\"}}");
                return;
            }

            for (PodSnapshot snapshot : snapshots) {
                AnomalyResult result = classifier.classify(snapshot, cycleState, properties.getThresholds());
                resourceLogger.log(snapshot, result);
            }
        } catch (Exception e) {
            log.warn("{{\"agent_status\":\"degraded\",\"error\":\"{}\"}}", e.getMessage());
        }
    }
}
