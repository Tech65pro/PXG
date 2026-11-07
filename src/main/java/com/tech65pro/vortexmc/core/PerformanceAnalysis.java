package com.tech65pro.vortexmc.core;

/** Outcome of comparing two measurement windows. */
public final class PerformanceAnalysis {

    public enum Trend {
        IMPROVING("Improving"),
        STABLE("Stable"),
        DEGRADING("Degrading");

        private final String label;

        Trend(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Severity {
        NONE("No change"),
        MINOR("Minor"),
        NOTICEABLE("Noticeable"),
        SEVERE("Severe");

        private final String label;

        Severity(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final double changePercent;
    private final Trend trend;
    private final Severity severity;
    private final boolean sustained;
    private final double confidence;
    private final double beforeAverage;
    private final double afterAverage;

    public PerformanceAnalysis(double beforeAverage,
                               double afterAverage,
                               double changePercent,
                               Trend trend,
                               Severity severity,
                               boolean sustained,
                               double confidence) {
        this.beforeAverage = beforeAverage;
        this.afterAverage = afterAverage;
        this.changePercent = changePercent;
        this.trend = trend;
        this.severity = severity;
        this.sustained = sustained;
        this.confidence = confidence;
    }

    /** Signed percentage change of the measured metric (positive is better for FPS). */
    public double changePercent() {
        return changePercent;
    }

    public Trend trend() {
        return trend;
    }

    public Severity severity() {
        return severity;
    }

    /** True when the effect held up across the whole post-change window rather than one spike. */
    public boolean sustained() {
        return sustained;
    }

    /** How much data backed this conclusion, 0..1. */
    public double confidence() {
        return confidence;
    }

    public double beforeAverage() {
        return beforeAverage;
    }

    public double afterAverage() {
        return afterAverage;
    }

    /** True when the measurement supports calling the change an improvement. */
    public boolean isMeasuredImprovement() {
        return changePercent > 2.0 && sustained;
    }

    /** True when the measurement shows the change made things worse. */
    public boolean isMeasuredRegression() {
        return changePercent < -2.0 && sustained;
    }

    public String describe() {
        if (severity == Severity.NONE) {
            return "No measurable change (" + String.format("%+.1f%%", changePercent) + ")";
        }
        return trend.label() + " " + String.format("%+.1f%%", changePercent)
                + (sustained ? ", sustained" : ", not sustained");
    }

    @Override
    public String toString() {
        return "PerformanceAnalysis[" + describe() + ", confidence " + String.format("%.0f%%", confidence * 100) + "]";
    }
}
