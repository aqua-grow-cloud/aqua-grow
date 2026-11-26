package com.aquagrow.resourcemonitor.rules;

import com.aquagrow.resourcemonitor.config.MonitorProperties;
import com.aquagrow.resourcemonitor.model.Classification;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AnomalyClassifier {

    private static final Set<String> ALERT_EVENT_REASONS = Set.of("OOMKilling", "Evicted");
    private static final Set<String> WARN_EVENT_REASONS = Set.of("BackOff", "Unhealthy");

    public AnomalyResult classify(PodSnapshot snapshot, CycleState state, MonitorProperties.Thresholds thresholds) {
        String key = snapshot.podName() + "/" + snapshot.containerName();
        List<String> alertReasons = new ArrayList<>();
        List<String> warnReasons = new ArrayList<>();

        evaluateMemory(snapshot, thresholds, alertReasons, warnReasons);
        evaluateCpu(snapshot, state, key, thresholds, warnReasons);
        evaluateRestarts(snapshot, state, key, alertReasons);
        evaluateReadiness(snapshot, state, key, thresholds, alertReasons);
        evaluateEvents(snapshot, alertReasons, warnReasons);

        if (!alertReasons.isEmpty()) {
            return new AnomalyResult(Classification.ALERT, alertReasons);
        }
        if (!warnReasons.isEmpty()) {
            return new AnomalyResult(Classification.WARN, warnReasons);
        }
        return new AnomalyResult(Classification.OK, List.of());
    }

    private void evaluateMemory(PodSnapshot snapshot, MonitorProperties.Thresholds thresholds,
                                 List<String> alertReasons, List<String> warnReasons) {
        if (snapshot.memUsageBytes() == null || snapshot.memLimitBytes() == null || snapshot.memLimitBytes() == 0) {
            return;
        }
        double pct = (snapshot.memUsageBytes() * 100.0) / snapshot.memLimitBytes();
        if (pct > thresholds.getMemAlertPct()) {
            alertReasons.add("mem_usage_pct_of_limit>" + thresholds.getMemAlertPct());
        } else if (pct > thresholds.getMemWarnPct()) {
            warnReasons.add("mem_usage_pct_of_limit>" + thresholds.getMemWarnPct());
        }
    }

    private void evaluateCpu(PodSnapshot snapshot, CycleState state, String key,
                              MonitorProperties.Thresholds thresholds, List<String> warnReasons) {
        if (snapshot.cpuUsageMillicores() == null || snapshot.cpuLimitMillicores() == null
                || snapshot.cpuLimitMillicores() == 0) {
            return;
        }
        double pct = (snapshot.cpuUsageMillicores() * 100.0) / snapshot.cpuLimitMillicores();
        if (pct > thresholds.getCpuWarnPct()) {
            state.incrementCpuHigh(key);
        } else {
            state.resetCpuHigh(key);
            return;
        }
        if (state.getCpuHighStreak(key) >= thresholds.getCpuWarnConsecutiveCycles()) {
            warnReasons.add("cpu_usage_pct_of_limit>" + thresholds.getCpuWarnPct()
                    + "_for_" + thresholds.getCpuWarnConsecutiveCycles() + "_cycles");
        }
    }

    private void evaluateRestarts(PodSnapshot snapshot, CycleState state, String key, List<String> alertReasons) {
        int previous = state.previousRestartCount(key, snapshot.restartCount());
        if (snapshot.restartCount() > previous) {
            alertReasons.add("restart_count_increased");
        }
    }

    private void evaluateReadiness(PodSnapshot snapshot, CycleState state, String key,
                                    MonitorProperties.Thresholds thresholds, List<String> alertReasons) {
        boolean unhealthyPhase = !"Running".equals(snapshot.phase());
        if (!snapshot.ready() || unhealthyPhase) {
            state.incrementNotReady(key);
        } else {
            state.resetNotReady(key);
            return;
        }
        if (state.getNotReadyStreak(key) >= thresholds.getNotReadyAlertConsecutiveCycles()) {
            alertReasons.add("not_ready_for_" + thresholds.getNotReadyAlertConsecutiveCycles() + "_cycles");
        }
    }

    private void evaluateEvents(PodSnapshot snapshot, List<String> alertReasons, List<String> warnReasons) {
        for (String reason : snapshot.recentEventReasons()) {
            if (ALERT_EVENT_REASONS.contains(reason)) {
                alertReasons.add("event:" + reason);
            } else if (WARN_EVENT_REASONS.contains(reason)) {
                warnReasons.add("event:" + reason);
            }
        }
    }
}
