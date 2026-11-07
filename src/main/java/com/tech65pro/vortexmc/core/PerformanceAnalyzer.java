package com.tech65pro.vortexmc.core;

/**
 * Turns raw samples into statements about what the frame rate is actually doing.
 *
 * <p>Everything here is deliberately conservative. A change is only called an improvement when
 * the effect is both large enough to matter and present throughout the measurement window, and
 * every result carries a confidence figure so the GUI can be honest about how thin the evidence
 * is. VortexMC never reports a gain it did not measure.</p>
 */
public final class PerformanceAnalyzer {

    /** Below this percentage change the result is reported as "no measurable change". */
    public static final double NOISE_THRESHOLD_PERCENT = 2.0;

    private PerformanceAnalyzer() {
    }

    /**
     * Compares two windows of the same metric.
     *
     * @param before samples collected before a change
     * @param after  samples collected after the change
     * @return an analysis, never {@code null}
     */
    public static PerformanceAnalysis compare(MetricSeries before, MetricSeries after) {
        double beforeAverage = before.hasSamples() ? before.average() : 0d;
        double afterAverage = after.hasSamples() ? after.average() : 0d;

        if (!before.hasSamples() || !after.hasSamples()) {
            return new PerformanceAnalysis(beforeAverage, afterAverage, 0d,
                    PerformanceAnalysis.Trend.STABLE, PerformanceAnalysis.Severity.NONE,
                    false, 0d);
        }

        double changePercent = 0d;
        if (beforeAverage > 0d) {
            changePercent = (afterAverage - beforeAverage) / beforeAverage * 100d;
        }

        double confidence = confidence(before, after);
        boolean sustained = isSustained(after);
        PerformanceAnalysis.Trend trend = trend(changePercent);
        PerformanceAnalysis.Severity severity = severity(changePercent);

        return new PerformanceAnalysis(beforeAverage, afterAverage, changePercent, trend, severity, sustained, confidence);
    }

    /**
     * Detects whether frame rate has fallen off a cliff recently.
     *
     * @param fpsSeries one-sample-per-second average FPS series
     * @param dropPercent how large a relative drop counts as a "drop"
     * @return the observed relative drop as a positive percentage, or 0 when there is not
     *         enough data to say anything
     */
    public static double detectDrop(MetricSeries fpsSeries, double dropPercent) {
        if (fpsSeries.sampleCount() < 6) {
            return 0d;
        }
        int recentCount = Math.max(2, fpsSeries.sampleCount() / 4);
        double recent = meanOfLast(fpsSeries, recentCount);
        double earlier = meanOfFirst(fpsSeries, fpsSeries.sampleCount() - recentCount);
        if (earlier <= 0d) {
            return 0d;
        }
        double drop = (earlier - recent) / earlier * 100d;
        return drop >= dropPercent ? drop : 0d;
    }

    /** True when the frame rate has fallen and stayed down for the whole recent window. */
    public static boolean isSustainedDrop(MetricSeries fpsSeries, double dropPercent) {
        double drop = detectDrop(fpsSeries, dropPercent);
        if (drop <= 0d) {
            return false;
        }
        int recentCount = Math.max(2, fpsSeries.sampleCount() / 4);
        double threshold = meanOfFirst(fpsSeries, fpsSeries.sampleCount() - recentCount) * (1.0 - dropPercent / 100.0);
        // every recent sample must sit below the threshold for the drop to count as sustained
        int start = Math.max(0, fpsSeries.sampleCount() - recentCount);
        for (int i = start; i < fpsSeries.sampleCount(); i++) {
            if (sampleAt(fpsSeries, i) > threshold) {
                return false;
            }
        }
        return true;
    }

    /** True when the frame time distribution is dominated by stalls rather than a slow average. */
    public static boolean isStuttering(FrameTimeStats stats, double medianRatio) {
        if (stats.sampleCount() < 60) {
            return false;
        }
        float median = stats.medianFrameTimeMillis();
        if (median <= 0f) {
            return false;
        }
        return stats.onePercentLowFps() > 0f
                && (stats.onePercentLowFps() / (1000f / median)) < medianRatio;
    }

    private static PerformanceAnalysis.Trend trend(double changePercent) {
        if (changePercent > NOISE_THRESHOLD_PERCENT) {
            return PerformanceAnalysis.Trend.IMPROVING;
        }
        if (changePercent < -NOISE_THRESHOLD_PERCENT) {
            return PerformanceAnalysis.Trend.DEGRADING;
        }
        return PerformanceAnalysis.Trend.STABLE;
    }

    private static PerformanceAnalysis.Severity severity(double changePercent) {
        double magnitude = Math.abs(changePercent);
        if (magnitude < NOISE_THRESHOLD_PERCENT) {
            return PerformanceAnalysis.Severity.NONE;
        }
        if (magnitude < 8.0) {
            return PerformanceAnalysis.Severity.MINOR;
        }
        if (magnitude < 20.0) {
            return PerformanceAnalysis.Severity.NOTICEABLE;
        }
        return PerformanceAnalysis.Severity.SEVERE;
    }

    private static boolean isSustained(MetricSeries series) {
        if (series.sampleCount() < 4) {
            return false;
        }
        // Compare the mean of the first and second halves of the post-change window: if the
        // effect was a single lucky spike the two halves will disagree.
        int half = series.sampleCount() / 2;
        double first = meanOfFirst(series, half);
        double second = meanOfLast(series, series.sampleCount() - half);
        if (first <= 0d || second <= 0d) {
            return false;
        }
        double spread = Math.abs(second - first) / first * 100d;
        return spread < 15.0;
    }

    private static double confidence(MetricSeries before, MetricSeries after) {
        int samples = Math.min(before.sampleCount(), after.sampleCount());
        double sampleConfidence = Math.min(1.0, samples / 20.0);
        double beforeStability = stability(before);
        double afterStability = stability(after);
        double stabilityConfidence = Math.max(0d, 1.0 - (beforeStability + afterStability) / 200.0);
        return clamp(sampleConfidence * 0.6 + stabilityConfidence * 0.4);
    }

    /** Coefficient of variation as a percentage; lower means steadier samples. */
    private static double stability(MetricSeries series) {
        if (series.sampleCount() < 2) {
            return 100d;
        }
        double mean = series.average();
        if (mean <= 0d) {
            return 100d;
        }
        double total = 0;
        for (int i = 0; i < series.sampleCount(); i++) {
            double delta = sampleAt(series, i) - mean;
            total += delta * delta;
        }
        return Math.sqrt(total / series.sampleCount()) / mean * 100d;
    }

    private static double meanOfFirst(MetricSeries series, int count) {
        int n = Math.min(count, series.sampleCount());
        if (n <= 0) {
            return 0d;
        }
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += sampleAt(series, i);
        }
        return total / n;
    }

    private static double meanOfLast(MetricSeries series, int count) {
        int n = Math.min(count, series.sampleCount());
        if (n <= 0) {
            return 0d;
        }
        double total = 0;
        for (int i = series.sampleCount() - n; i < series.sampleCount(); i++) {
            total += sampleAt(series, i);
        }
        return total / n;
    }

    private static double sampleAt(MetricSeries series, int logicalIndex) {
        return series.entryAtLogical(logicalIndex);
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }
}
