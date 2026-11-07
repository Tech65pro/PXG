package com.tech65pro.vortexmc.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comparing two measurement windows. */
class PerformanceAnalyzerTest {

    private static MetricSeries window(double value, int samples) {
        MetricSeries series = new MetricSeries(samples);
        for (int i = 0; i < samples; i++) {
            series.add(i * 1000L, value);
        }
        return series;
    }

    @Test
    @DisplayName("a real improvement is reported as one")
    void improvementIsReported() {
        PerformanceAnalysis analysis = PerformanceAnalyzer.compare(window(50, 30), window(60, 30));
        Assertions.assertEquals(20d, analysis.changePercent(), 0.01d);
        Assertions.assertEquals(PerformanceAnalysis.Trend.IMPROVING, analysis.trend());
        Assertions.assertTrue(analysis.sustained(), "a steady window is sustained");
        Assertions.assertTrue(analysis.isMeasuredImprovement());
        Assertions.assertTrue(analysis.confidence() > 0.9, "two steady windows give high confidence");
    }

    @Test
    @DisplayName("a real regression is reported as one")
    void regressionIsReported() {
        PerformanceAnalysis analysis = PerformanceAnalyzer.compare(window(60, 30), window(45, 30));
        Assertions.assertEquals(-25d, analysis.changePercent(), 0.01d);
        Assertions.assertEquals(PerformanceAnalysis.Trend.DEGRADING, analysis.trend());
        Assertions.assertTrue(analysis.isMeasuredRegression());
        Assertions.assertEquals(PerformanceAnalysis.Severity.SEVERE, analysis.severity());
    }

    @Test
    @DisplayName("a change inside the noise floor is reported as no measurable change")
    void noiseIsNotAnImprovement() {
        PerformanceAnalysis analysis = PerformanceAnalyzer.compare(window(50, 30), window(50.5, 30));
        Assertions.assertEquals(PerformanceAnalysis.Trend.STABLE, analysis.trend());
        Assertions.assertEquals(PerformanceAnalysis.Severity.NONE, analysis.severity());
        Assertions.assertFalse(analysis.isMeasuredImprovement());
        Assertions.assertFalse(analysis.isMeasuredRegression());
    }

    @Test
    @DisplayName("a one-off spike is not called an improvement")
    void spikeIsNotAnImprovement() {
        MetricSeries spiky = new MetricSeries(30);
        for (int i = 0; i < 15; i++) {
            spiky.add(i * 1000L, 60);
        }
        for (int i = 15; i < 30; i++) {
            spiky.add(i * 1000L, 140);
        }
        PerformanceAnalysis analysis = PerformanceAnalyzer.compare(window(50, 30), spiky);
        Assertions.assertTrue(analysis.changePercent() > 2.0, "the average does move");
        Assertions.assertFalse(analysis.sustained(), "but the two halves disagree");
        Assertions.assertFalse(analysis.isMeasuredImprovement(),
                "a spike that does not hold must not be reported as a gain");
    }

    @Test
    @DisplayName("stutter is detected from an uneven frame time distribution")
    void stutterIsDetected() {
        FrameTimeStats stuttery = new FrameTimeStats(600);
        for (int i = 0; i < 600; i++) {
            stuttery.add(i % 10 == 0 ? 120f : 12f);
        }
        FrameTimeStats smooth = new FrameTimeStats(600);
        for (int i = 0; i < 600; i++) {
            smooth.add(16f);
        }
        Assertions.assertTrue(PerformanceAnalyzer.isStuttering(stuttery, 0.7));
        Assertions.assertFalse(PerformanceAnalyzer.isStuttering(smooth, 0.7));
    }

    @Test
    @DisplayName("too few frames means no stutter verdict")
    void stutterNeedsData() {
        FrameTimeStats tiny = new FrameTimeStats(600);
        for (int i = 0; i < 10; i++) {
            tiny.add(200f);
        }
        Assertions.assertFalse(PerformanceAnalyzer.isStuttering(tiny, 0.7));
    }
}
