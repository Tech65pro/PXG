package com.tech65pro.vortexmc.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rolling time-series statistics. */
class MetricSeriesTest {

    private MetricSeries series;

    @BeforeEach
    void setUp() {
        series = new MetricSeries(60);
    }

    @Test
    @DisplayName("logical access runs oldest to newest")
    void logicalAccessIsOrdered() {
        for (int i = 0; i < 60; i++) {
            series.add(i * 1000L, i);
        }
        Assertions.assertEquals(60, series.sampleCount());
        Assertions.assertEquals(0d, series.entryAtLogical(0), 0.001d, "index 0 is the oldest sample");
        Assertions.assertEquals(59d, series.entryAtLogical(59), 0.001d, "the last index is the newest");
        Assertions.assertEquals(0d, series.oldest(), 0.001d);
        Assertions.assertEquals(59d, series.newest(), 0.001d);
        Assertions.assertEquals(59d, series.latest(), 0.001d);
    }

    @Test
    @DisplayName("the slope is negative when a metric is falling")
    void slopeIsNegativeForAFallingMetric() {
        for (int i = 0; i < 30; i++) {
            series.add(i * 1000L, 60);
        }
        for (int i = 30; i < 60; i++) {
            series.add(i * 1000L, 30);
        }
        Assertions.assertTrue(series.slopePerSecond() < -0.5, "a step from 60 to 30 must slope down");
    }

    @Test
    @DisplayName("a flat metric has no slope and no detected drop")
    void flatMetricIsStable() {
        for (int i = 0; i < 60; i++) {
            series.add(i * 1000L, 60);
        }
        Assertions.assertEquals(0d, series.slopePerSecond(), 0.001d);
        Assertions.assertEquals(0d, PerformanceAnalyzer.detectDrop(series, 15.0), 0.001d);
        Assertions.assertFalse(PerformanceAnalyzer.isSustainedDrop(series, 15.0));
    }

    @Test
    @DisplayName("a sustained drop is detected, a brief dip is not")
    void dropDetectionDistinguishesSustainedFromBrief() {
        for (int i = 0; i < 45; i++) {
            series.add(i * 1000L, 60);
        }
        for (int i = 45; i < 60; i++) {
            series.add(i * 1000L, 20);
        }
        Assertions.assertTrue(PerformanceAnalyzer.detectDrop(series, 15.0) > 0d);
        Assertions.assertTrue(PerformanceAnalyzer.isSustainedDrop(series, 15.0));

        MetricSeries brief = new MetricSeries(60);
        for (int i = 0; i < 60; i++) {
            brief.add(i * 1000L, i >= 28 && i <= 31 ? 20 : 60);
        }
        Assertions.assertFalse(PerformanceAnalyzer.isSustainedDrop(brief, 15.0),
                "a four-second dip must not count as a sustained drop");
    }

    @Test
    @DisplayName("out-of-range logical reads return zero instead of throwing")
    void outOfRangeReadsAreSafe() {
        series.add(0L, 5d);
        Assertions.assertEquals(0d, series.entryAtLogical(-1), 0.001d);
        Assertions.assertEquals(0d, series.entryAtLogical(99), 0.001d);
    }

    @Test
    @DisplayName("too little data produces no verdict rather than a wrong one")
    void tooLittleDataProducesNoVerdict() {
        Assertions.assertEquals(0d, PerformanceAnalyzer.detectDrop(series, 15.0), 0.001d);
        PerformanceAnalysis analysis = PerformanceAnalyzer.compare(series, new MetricSeries(10));
        Assertions.assertEquals(0d, analysis.confidence(), 0.001d);
        Assertions.assertFalse(analysis.isMeasuredImprovement());
    }
}
