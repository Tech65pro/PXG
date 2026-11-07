package com.tech65pro.vortexmc.core;

/**
 * Fixed-size ring buffer of time-stamped samples.
 *
 * <p>Used for anything sampled at a coarse cadence (average FPS once per second, heap usage,
 * entity counts) where a rolling statistic over a window is more informative than the
 * instantaneous value.</p>
 */
public final class MetricSeries {

    private final double[] values;
    private final long[] timestamps;
    private int writeIndex;
    private int filled;

    public MetricSeries(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.values = new double[capacity];
        this.timestamps = new long[capacity];
    }

    public void add(long timestampMillis, double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        values[writeIndex] = value;
        timestamps[writeIndex] = timestampMillis;
        writeIndex = (writeIndex + 1) % values.length;
        if (filled < values.length) {
            filled++;
        }
    }

    public void clear() {
        writeIndex = 0;
        filled = 0;
        java.util.Arrays.fill(values, 0d);
        java.util.Arrays.fill(timestamps, 0L);
    }

    public int capacity() {
        return values.length;
    }

    public int sampleCount() {
        return filled;
    }

    public boolean hasSamples() {
        return filled > 0;
    }

    public double latest() {
        if (filled == 0) {
            return 0d;
        }
        return values[(writeIndex - 1 + values.length) % values.length];
    }

    public long latestTimestamp() {
        if (filled == 0) {
            return 0L;
        }
        return timestamps[(writeIndex - 1 + values.length) % values.length];
    }

    /** Oldest sample currently retained. */
    public double oldest() {
        return entryAtLogical(0);
    }

    /** Newest sample currently retained. */
    public double newest() {
        return entryAtLogical(filled - 1);
    }

    /**
     * Logical access into the window: index {@code 0} is the oldest retained sample and
     * {@code sampleCount() - 1} is the newest.
     */
    public double entryAtLogical(int logicalIndex) {
        if (logicalIndex < 0 || logicalIndex >= filled) {
            return 0d;
        }
        int capacity = values.length;
        int oldest = (writeIndex - filled + capacity) % capacity;
        return values[(oldest + logicalIndex) % capacity];
    }

    public double average() {
        if (filled == 0) {
            return 0d;
        }
        double total = 0;
        for (int i = 0; i < filled; i++) {
            total += values[i];
        }
        return total / filled;
    }

    public double min() {
        if (filled == 0) {
            return 0d;
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < filled; i++) {
            if (values[i] < min) {
                min = values[i];
            }
        }
        return min;
    }

    public double max() {
        if (filled == 0) {
            return 0d;
        }
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < filled; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    /** Mean of the second half minus the mean of the first half. Positive means improving. */
    public double halfOverHalfDelta() {
        if (filled < 4) {
            return 0d;
        }
        int half = filled / 2;
        double first = 0;
        double second = 0;
        int start = (writeIndex - filled + values.length) % values.length;
        for (int i = 0; i < half; i++) {
            first += values[(start + i) % values.length];
        }
        for (int i = half; i < filled; i++) {
            second += values[(start + i) % values.length];
        }
        return (second / (filled - half)) - (first / half);
    }

    /**
     * Least-squares slope in units-per-second. Positive means the metric is trending up.
     * Returns 0 when there is not enough spread in the timestamps to fit a line.
     */
    public double slopePerSecond() {
        if (filled < 3) {
            return 0d;
        }
        int start = (writeIndex - filled + values.length) % values.length;
        double t0 = timestamps[start] / 1000d;
        double sumT = 0;
        double sumY = 0;
        double sumTT = 0;
        double sumTY = 0;
        int n = 0;
        for (int i = 0; i < filled; i++) {
            int index = (start + i) % values.length;
            double t = timestamps[index] / 1000d - t0;
            double y = values[index];
            sumT += t;
            sumY += y;
            sumTT += t * t;
            sumTY += t * y;
            n++;
        }
        double denominator = n * sumTT - sumT * sumT;
        if (Math.abs(denominator) < 1e-9) {
            return 0d;
        }
        return (n * sumTY - sumT * sumY) / denominator;
    }
}
