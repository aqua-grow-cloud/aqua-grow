package com.aquagrow.resourcemonitor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "monitor.namespace=test-ns",
        "monitor.poll-interval-seconds=30",
        "monitor.selectors[0]=app=foo",
        "monitor.selectors[1]=app=bar",
        "monitor.thresholds.mem-warn-pct=80",
        "monitor.thresholds.mem-alert-pct=90",
        "monitor.thresholds.cpu-warn-pct=85",
        "monitor.thresholds.cpu-warn-consecutive-cycles=3",
        "monitor.thresholds.not-ready-alert-consecutive-cycles=4"
})
class MonitorPropertiesTest {

    @Resource
    MonitorProperties properties;

    @Test
    void bindsAllPropertiesFromConfig() {
        assertThat(properties.getNamespace()).isEqualTo("test-ns");
        assertThat(properties.getPollIntervalSeconds()).isEqualTo(30);
        assertThat(properties.getSelectors()).containsExactly("app=foo", "app=bar");
        assertThat(properties.getThresholds().getMemWarnPct()).isEqualTo(80);
        assertThat(properties.getThresholds().getMemAlertPct()).isEqualTo(90);
        assertThat(properties.getThresholds().getCpuWarnPct()).isEqualTo(85);
        assertThat(properties.getThresholds().getCpuWarnConsecutiveCycles()).isEqualTo(3);
        assertThat(properties.getThresholds().getNotReadyAlertConsecutiveCycles()).isEqualTo(4);
    }
}
