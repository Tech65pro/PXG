package com.tech65pro.vortexmc.core;

/**
 * Rolling statistics over recent frame times.
 *
 * <p>Minecraft already exposes a smoothed FPS counter, but a single number hides stutter. This
 * class keeps a bounded ring buffer of raw frame times and derives the numbers that actually
 * describe how a session <em>feels</em>: average frame time, median, the 1% and 0.1% lows, and
 * the worst single frame. It allocates nothing after construction so it can be fed every frame
 * without contributing to garbage-collection pauses.</p>
 */
public final class FrameTimeStats {

    private final float[] samples;
    private int writeIndex;
    private int filled;

    public FrameTimeStats(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        this.samples = new float[capacity];
    }

    /** Records one frame time in milliseconds. Non-finite or non-positive values are ignored. */
    public void add(float frameTimeMillis) {
        if (!Float.isFinite(frameTimeMillis) || frameTimeMillis <= 0f) {
            return;
        }
        samples[writeIndex] = frameTimeMillis;
        writeIndex = (writeIndex + 1) % samples.length;
        if (filled < samples.length) {
            filled++;
        }
    }

    public void clear() {
        writeIndex = 0;
        filled = 0;
        java.util.Arrays.fill(samples, 0f);
    }

    public int capacity() {
        return samples.length;
    }

    public int sampleCount() {
        return filled;
    }

    public boolean hasSamples() {
        return filled > 0;
    }

    /** Oldest frame time still retained, or 0 when nothing has been recorded yet. */
    public float oldestFrameTimeMillis() {
        if (filled == 0) {
            return 0f;
        }
        int oldest = (filled == samples.length) ? writeIndex : 0;
        return samples[oldest];
    }

    /** Most recent frame time, or 0 when nothing has been recorded yet. */
    public float lastFrameTimeMillis() {
        if (filled == 0) {
            return 0f;
        }
        int index = (writeIndex - 1 + samples.length) % samples.length;
        return samples[index];
    }

    /** Frames per second implied by the most recent frame time. */
    public float instantFps() {
        float last = lastFrameTimeMillis();
        return last > 0f ? 1000f / last : 0f;
    }

    public float averageFrameTimeMillis() {
        if (filled == 0) {
            return 0f;
        }
        double total = 0;
        for (int i = 0; i < filled; i++) {
            total += samples[i];
        }
        return (float) (total / filled);
    }

    public float averageFps() {
        float avg = averageFrameTimeMillis();
        return avg > 0f ? 1000f / avg : 0f;
    }

    public float medianFrameTimeMillis() {
        return percentile(0.5);
    }

    public float percentile(double fraction) {
        if (filled == 0) {
            return 0f;
        }
        float[] sorted = sortedCopy();
        int index = (int) Math.floor(fraction * (sorted.length - 1));
        if (index < 0) {
            index = 0;
        }
        if (index > sorted.length - 1) {
            index = sorted.length - 1;
        }
        return sorted[index];
    }

    public float worstFrameTimeMillis() {
        if (filled == 0) {
            return 0f;
        }
        float worst = 0f;
        for (int i = 0; i < filled; i++) {
            if (samples[i] > worst) {
                worst = samples[i];
            }
        }
        return worst;
    }

    public float bestFrameTimeMillis() {
        if (filled == 0) {
            return 0f;
        }
        float best = Float.MAX_VALUE;
        for (int i = 0; i < filled; i++) {
            if (samples[i] < best) {
                best = samples[i];
            }
        }
        return best;
    }

    public float standardDeviationMillis() {
        if (filled < 2) {
            return 0f;
        }
        float mean = averageFrameTimeMillis();
        double total = 0;
        for (int i = 0; i < filled; i++) {
            double delta = samples[i] - mean;
            total += delta * delta;
        }
        return (float) Math.sqrt(total / (filled - 1));
    }

    /**
     * The "1% low": the average frame rate of the slowest 1% of frames in the window.
     * This is the number that correlates best with perceived stutter.
     */
    public float onePercentLowFps() {
        return lowFps(0.01);
    }

    /** The "0.1% low", useful for spotting individual hard stalls. */
    public float zeroPointOnePercentLowFps() {
        return lowFps(0.001);
    }

    private float lowFps(double fraction) {
        if (filled == 0) {
            return 0f;
        }
        float[] sorted = sortedCopy();
        int worstCount = Math.max(1, (int) Math.ceil(sorted.length * fraction));
        worstCount = Math.min(worstCount, sorted.length);
        double total = 0;
        for (int i = 0; i < worstCount; i++) {
            total += sorted[sorted.length - 1 - i];
        }
        float worstAverage = (float) (total / worstCount);
        return worstAverage > 0f ? 1000f / worstAverage : 0f;
    }

    private float[] sortedCopy() {
        float[] copy = new float[filled];
        System.arraycopy(samples, 0, copy, 0, filled);
        java.util.Arrays.sort(copy);
        return copy;
    }
}
