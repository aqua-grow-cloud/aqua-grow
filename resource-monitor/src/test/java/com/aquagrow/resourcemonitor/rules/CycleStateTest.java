package com.aquagrow.resourcemonitor.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CycleStateTest {

    @Test
    void cpuHighStreakIncrementsAndResets() {
        CycleState state = new CycleState();
        assertThat(state.getCpuHighStreak("pod-a")).isZero();

        state.incrementCpuHigh("pod-a");
        state.incrementCpuHigh("pod-a");
        assertThat(state.getCpuHighStreak("pod-a")).isEqualTo(2);

        state.resetCpuHigh("pod-a");
        assertThat(state.getCpuHighStreak("pod-a")).isZero();
    }

    @Test
    void notReadyStreakIncrementsAndResets() {
        CycleState state = new CycleState();
        state.incrementNotReady("pod-b");
        state.incrementNotReady("pod-b");
        state.incrementNotReady("pod-b");
        assertThat(state.getNotReadyStreak("pod-b")).isEqualTo(3);

        state.resetNotReady("pod-b");
        assertThat(state.getNotReadyStreak("pod-b")).isZero();
    }

    @Test
    void previousRestartCountReturnsZeroFirstTimeThenTracksIncreases() {
        CycleState state = new CycleState();

        int first = state.previousRestartCount("pod-c", 0);
        assertThat(first).isZero();

        int second = state.previousRestartCount("pod-c", 2);
        assertThat(second).isZero(); // returns value BEFORE this update

        int third = state.previousRestartCount("pod-c", 2);
        assertThat(third).isEqualTo(2); // no change since last call
    }
}
