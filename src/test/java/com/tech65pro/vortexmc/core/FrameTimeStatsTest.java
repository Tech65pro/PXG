package com.tech65pro.vortexmc.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Frame-time statistics over a rolling window. */
class FrameTimeStatsTest {

    private FrameTimeStats stats;

    @BeforeEach
    void setUp() {
        stats = new FrameTimeStats(600);
    }

    @Test
    @DisplayName("an empty window reports no data rather than zeroes")
    void emptyWindowHasNoSamples() {
        Assertions.assertFalse(stats.hasSamples());
        Assertions.assertEquals(0, stats.sampleCount());
        Assertions.assertEquals(0f, stats.averageFps(), 0.001f, "no samples means no frame rate");
        Assertions.assertEquals(0f, stats.lastFrameTimeMillis(), 0.001f, "no samples means no frame time");
    }

    @Test
    @DisplayName("a steady 16 ms frame time averages 62.5 fps")
    void steadyFrameTimeGivesExpectedAverage() {
        for (int i = 0; i < 600; i++) {
            stats.add(16.0f);
        }
        Assertions.assertEquals(62.5f, stats.averageFps(), 0.5f);
        Assertions.assertEquals(62.5f, stats.instantFps(), 0.1f);
        Assertions.assertEquals(16f, stats.medianFrameTimeMillis(), 0.001f);
        Assertions.assertEquals(0f, stats.standardDeviationMillis(), 0.001f);
    }

    @Test
    @DisplayName("the 1% low collapses when slow frames are added")
    void onePercentLowDetectsStalls() {
        for (int i = 0; i < 600; i++) {
            stats.add(16.0f);
        }
        float before = stats.onePercentLowFps();
        for (int i = 0; i < 6; i++) {
            stats.add(200.0f);
        }
        Assertions.assertTrue(before > 60f, "steady frames should give a high 1% low");
        Assertions.assertTrue(stats.onePercentLowFps() < 20f, "stalls must pull the 1% low down");
        Assertions.assertTrue(stats.averageFps() < 62.5f, "stalls must pull the average down too");
        Assertions.assertEquals(200f, stats.worstFrameTimeMillis(), 0.001f);
    }

    @Test
    @DisplayName("non-finite and non-positive frame times are ignored")
    void nonsenseSamplesAreIgnored() {
        stats.add(Float.NaN);
        stats.add(Float.POSITIVE_INFINITY);
        stats.add(0f);
        stats.add(-5f);
        Assertions.assertFalse(stats.hasSamples(), "invalid samples must not be recorded");
    }

    @Test
    @DisplayName("the ring buffer keeps only the newest samples")
    void ringBufferKeepsNewest() {
        FrameTimeStats small = new FrameTimeStats(4);
        small.add(10f);
        small.add(20f);
        small.add(30f);
        small.add(40f);
        small.add(50f);
        Assertions.assertEquals(4, small.sampleCount(), "capacity is respected");
        Assertions.assertEquals(50f, small.lastFrameTimeMillis(), 0.001f, "the newest sample wins");
        Assertions.assertEquals(20f, small.oldestFrameTimeMillis(), 0.001f, "the oldest sample has rolled off");
    }

    @Test
    @DisplayName("the 0.1% low is never above the 1% low")
    void zeroPointOneIsBelowOnePercent() {
        for (int i = 0; i < 600; i++) {
            stats.add(i % 7 == 0 ? 90f : 16f);
        }
        Assertions.assertTrue(stats.zeroPointOnePercentLowFps() <= stats.onePercentLowFps() + 0.001f);
    }
}
