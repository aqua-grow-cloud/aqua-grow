package com.aquagrow.resourcemonitor;

import com.aquagrow.resourcemonitor.config.MonitorProperties;
import com.aquagrow.resourcemonitor.k8s.PodMetricsClient;
import com.aquagrow.resourcemonitor.logging.ResourceLogger;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import com.aquagrow.resourcemonitor.rules.AnomalyClassifier;
import com.aquagrow.resourcemonitor.rules.CycleState;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorLoopSchedulerTest {

    private MonitorProperties properties() {
        MonitorProperties props = new MonitorProperties();
        props.setNamespace("aqua-grow");
        props.setSelectors(List.of("app=aqua-grow-api"));
        props.setPollIntervalSeconds(60);
        MonitorProperties.Thresholds t = new MonitorProperties.Thresholds();
        t.setMemWarnPct(85);
        t.setMemAlertPct(95);
        t.setCpuWarnPct(90);
        t.setCpuWarnConsecutiveCycles(2);
        t.setNotReadyAlertConsecutiveCycles(3);
        props.setThresholds(t);
        return props;
    }

    @Test
    void runOnceLogsOneEntryPerSnapshotAndNeverThrowsOnClientFailure() {
        PodMetricsClient client = mock(PodMetricsClient.class);
        ResourceLogger logger = mock(ResourceLogger.class);
        MonitorLoopScheduler scheduler = new MonitorLoopScheduler(
                client, new AnomalyClassifier(), new CycleState(), logger, properties());

        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api", 200L, 300_000_000L, 500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());
        when(client.fetchSnapshots("aqua-grow", List.of("app=aqua-grow-api")))
                .thenReturn(List.of(snapshot));

        scheduler.runOnce();

        verify(logger, times(1)).log(eq(snapshot), any());
    }

    @Test
    void runOnceDoesNotThrowWhenClientFails() {
        PodMetricsClient client = mock(PodMetricsClient.class);
        ResourceLogger logger = mock(ResourceLogger.class);
        MonitorLoopScheduler scheduler = new MonitorLoopScheduler(
                client, new AnomalyClassifier(), new CycleState(), logger, properties());

        when(client.fetchSnapshots(any(), any())).thenThrow(new RuntimeException("api unreachable"));

        assertThat(catchThrowableAsSuccess(scheduler::runOnce)).isTrue();
        verifyNoInteractions(logger);
    }

    private boolean catchThrowableAsSuccess(Runnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
