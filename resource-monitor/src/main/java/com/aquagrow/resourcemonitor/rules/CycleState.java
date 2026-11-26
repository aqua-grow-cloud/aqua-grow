package com.aquagrow.resourcemonitor.rules;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class CycleState {

    private final Map<String, Integer> cpuHighStreaks = new ConcurrentHashMap<>();
    private final Map<String, Integer> notReadyStreaks = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastRestartCounts = new ConcurrentHashMap<>();

    public void incrementCpuHigh(String key) {
        cpuHighStreaks.merge(key, 1, Integer::sum);
    }

    public void resetCpuHigh(String key) {
        cpuHighStreaks.put(key, 0);
    }

    public int getCpuHighStreak(String key) {
        return cpuHighStreaks.getOrDefault(key, 0);
    }

    public void incrementNotReady(String key) {
        notReadyStreaks.merge(key, 1, Integer::sum);
    }

    public void resetNotReady(String key) {
        notReadyStreaks.put(key, 0);
    }

    public int getNotReadyStreak(String key) {
        return notReadyStreaks.getOrDefault(key, 0);
    }

    /**
     * Returns the restart count observed on the PREVIOUS call for this key
     * (0 if never seen before), then records currentCount as the new baseline.
     */
    public int previousRestartCount(String key, int currentCount) {
        Integer previous = lastRestartCounts.put(key, currentCount);
        return previous == null ? 0 : previous;
    }
}
