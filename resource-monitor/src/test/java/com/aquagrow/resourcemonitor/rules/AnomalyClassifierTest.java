package com.aquagrow.resourcemonitor.rules;

import com.aquagrow.resourcemonitor.config.MonitorProperties;
import com.aquagrow.resourcemonitor.model.Classification;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyClassifierTest {

    private MonitorProperties.Thresholds defaultThresholds() {
        MonitorProperties.Thresholds t = new MonitorProperties.Thresholds();
        t.setMemWarnPct(85);
        t.setMemAlertPct(95);
        t.setCpuWarnPct(90);
        t.setCpuWarnConsecutiveCycles(2);
        t.setNotReadyAlertConsecutiveCycles(3);
        return t;
    }

    @Test
    void healthyPodClassifiesAsOk() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L,   // usage
                500L, 512_000_000L,   // limit
                0, true, "Running", Collections.emptyList());

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.OK);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void memoryOverAlertThresholdClassifiesAsAlert() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        // 500Mi usage / 512Mi limit = ~97.6%
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 500_000_000L,
                500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.ALERT);
        assertThat(result.reasons()).contains("mem_usage_pct_of_limit>95");
    }

    @Test
    void memoryOverWarnButUnderAlertClassifiesAsWarn() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        // 450Mi / 512Mi = ~87.9%
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 450_000_000L,
                500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.WARN);
        assertThat(result.reasons()).contains("mem_usage_pct_of_limit>85");
    }

    @Test
    void cpuHighOnlyOneCycleDoesNotAlertYet() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        // 480m / 500m = 96% > 90% warn threshold, but only 1st cycle
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                480L, 300_000_000L,
                500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.OK);
    }

    @Test
    void cpuHighForTwoConsecutiveCyclesClassifiesAsWarn() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                480L, 300_000_000L,
                500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());

        classifier.classify(snapshot, state, defaultThresholds()); // cycle 1
        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds()); // cycle 2

        assertThat(result.level()).isEqualTo(Classification.WARN);
        assertThat(result.reasons()).contains("cpu_usage_pct_of_limit>90_for_2_cycles");
    }

    @Test
    void restartCountIncreaseClassifiesAsAlert() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot first = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L, 500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());
        PodSnapshot second = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L, 500L, 512_000_000L,
                1, true, "Running", Collections.emptyList());

        classifier.classify(first, state, defaultThresholds());
        AnomalyResult result = classifier.classify(second, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.ALERT);
        assertThat(result.reasons()).contains("restart_count_increased");
    }

    @Test
    void notReadyForThreeConsecutiveCyclesClassifiesAsAlert() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot notReady = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L, 500L, 512_000_000L,
                0, false, "Running", Collections.emptyList());

        classifier.classify(notReady, state, defaultThresholds()); // 1
        classifier.classify(notReady, state, defaultThresholds()); // 2
        AnomalyResult result = classifier.classify(notReady, state, defaultThresholds()); // 3

        assertThat(result.level()).isEqualTo(Classification.ALERT);
        assertThat(result.reasons()).contains("not_ready_for_3_cycles");
    }

    @Test
    void oomKillingEventClassifiesAsAlert() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L, 500L, 512_000_000L,
                0, true, "Running", List.of("OOMKilling"));

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.ALERT);
        assertThat(result.reasons()).contains("event:OOMKilling");
    }

    @Test
    void backOffEventClassifiesAsWarn() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L, 500L, 512_000_000L,
                0, true, "Running", List.of("BackOff"));

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.WARN);
        assertThat(result.reasons()).contains("event:BackOff");
    }

    @Test
    void noLimitSetSkipsPercentageCalcWithoutAnomaly() {
        AnomalyClassifier classifier = new AnomalyClassifier();
        CycleState state = new CycleState();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L,
                null, null, // no limits set
                0, true, "Running", Collections.emptyList());

        AnomalyResult result = classifier.classify(snapshot, state, defaultThresholds());

        assertThat(result.level()).isEqualTo(Classification.OK);
    }
}
