package com.aquagrow.resourcemonitor;

import com.aquagrow.resourcemonitor.config.MonitorProperties;
import com.aquagrow.resourcemonitor.k8s.PodMetricsClient;
import com.aquagrow.resourcemonitor.logging.ResourceLogger;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import com.aquagrow.resourcemonitor.rules.AnomalyClassifier;
import com.aquagrow.resourcemonitor.rules.CycleState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorLoopSchedulerTest {

    private ListAppender<ILoggingEvent> logAppender;
    private Logger schedulerLogger;

    @BeforeEach
    void setUpLogCapture() {
        schedulerLogger = (Logger) LoggerFactory.getLogger(MonitorLoopScheduler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        schedulerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDownLogCapture() {
        schedulerLogger.detachAppender(logAppender);
    }

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
        when(client.fetchSnapshotsBySelector("aqua-grow", List.of("app=aqua-grow-api")))
                .thenReturn(Map.of("app=aqua-grow-api", List.of(snapshot)));

        scheduler.runOnce();

        verify(logger, times(1)).log(eq(snapshot), any());
    }

    @Test
    void runOnceDoesNotThrowWhenClientFails() {
        PodMetricsClient client = mock(PodMetricsClient.class);
        ResourceLogger logger = mock(ResourceLogger.class);
        MonitorLoopScheduler scheduler = new MonitorLoopScheduler(
                client, new AnomalyClassifier(), new CycleState(), logger, properties());

        when(client.fetchSnapshotsBySelector(any(), any())).thenThrow(new RuntimeException("api unreachable"));

        assertThat(catchThrowableAsSuccess(scheduler::runOnce)).isTrue();
        verifyNoInteractions(logger);

        ObjectMapper mapper = new ObjectMapper();
        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        JsonNode json = parseOrFail(mapper, messages.get(0));
        assertThat(json.get("agent_status").asText()).isEqualTo("degraded");
        assertThat(json.get("error").asText()).isEqualTo("api unreachable");
    }

    @Test
    void runOnceLogsPerSelectorWhenOnlyOneSelectorMatchesZeroPods() {
        PodMetricsClient client = mock(PodMetricsClient.class);
        ResourceLogger logger = mock(ResourceLogger.class);
        MonitorProperties props = properties();
        props.setSelectors(List.of("app=aqua-grow-api", "app=postgres-api"));
        MonitorLoopScheduler scheduler = new MonitorLoopScheduler(
                client, new AnomalyClassifier(), new CycleState(), logger, props);

        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api", 200L, 300_000_000L, 500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());
        when(client.fetchSnapshotsBySelector("aqua-grow", List.of("app=aqua-grow-api", "app=postgres-api")))
                .thenReturn(Map.of(
                        "app=aqua-grow-api", List.of(snapshot),
                        "app=postgres-api", List.of()));

        scheduler.runOnce();

        verify(logger, times(1)).log(eq(snapshot), any());

        ObjectMapper mapper = new ObjectMapper();
        List<String> messages = logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).hasSize(1);
        JsonNode json = parseOrFail(mapper, messages.get(0));
        assertThat(json.get("agent_status").asText()).isEqualTo("no pods matched selector");
        assertThat(json.get("selector").asText()).isEqualTo("app=postgres-api");
    }

    private JsonNode parseOrFail(ObjectMapper mapper, String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new AssertionError("Expected valid JSON but got: " + json, e);
        }
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
